package io.github.vitaa1.vencefacil.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.vitaa1.vencefacil.inventory.BusinessDateProvider;
import io.github.vitaa1.vencefacil.inventory.ProductNameNormalizer;

@Service
class DemoDataService {

	private static final String INSTANCE_ID_KEY = "demo_instance_id";
	private static final String LAST_RESET_KEY = "demo_last_reset_at";
	private static final String ADVISORY_LOCK_NAME = "vence-facil-demo-reset";

	private final Clock clock;
	private final BusinessDateProvider businessDateProvider;
	private final DemoProperties properties;
	private final JdbcTemplate jdbcTemplate;
	private volatile Instant nextResetAt = Instant.EPOCH;

	DemoDataService(Clock clock, BusinessDateProvider businessDateProvider, DemoProperties properties,
			JdbcTemplate jdbcTemplate) {
		this.clock = clock;
		this.businessDateProvider = businessDateProvider;
		this.properties = properties;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public void resetIfDue() {
		if (!properties.enabled() || clock.instant().isBefore(nextResetAt)) {
			return;
		}

		jdbcTemplate.query("select pg_advisory_xact_lock(hashtext(?))",
				statement -> statement.setString(1, ADVISORY_LOCK_NAME), resultSet -> null);
		verifyOrInitializeDemoMarker();

		Instant now = clock.instant();
		Optional<Instant> lastReset = metadata(LAST_RESET_KEY).map(Instant::parse);
		if (lastReset.isPresent() && now.isBefore(lastReset.get().plus(properties.resetAfter()))) {
			nextResetAt = lastReset.get().plus(properties.resetAfter());
			return;
		}

		resetInventory(now);
		upsertMetadata(LAST_RESET_KEY, now.toString(), now);
		nextResetAt = now.plus(properties.resetAfter());
	}

	private void verifyOrInitializeDemoMarker() {
		Optional<String> marker = metadata(INSTANCE_ID_KEY);
		if (marker.isPresent()) {
			if (!marker.get().equals(properties.instanceId())) {
				throw new IllegalStateException("Demo database marker does not match DEMO_INSTANCE_ID");
			}
			return;
		}

		Long inventoryRecords = jdbcTemplate.queryForObject("""
				select (select count(*) from products)
				     + (select count(*) from stock_entries)
				     + (select count(*) from stock_movements)
				""", Long.class);
		if (inventoryRecords == null || inventoryRecords != 0) {
			throw new IllegalStateException("Demo mode can only initialize an empty, dedicated database");
		}

		Instant now = clock.instant();
		upsertMetadata(INSTANCE_ID_KEY, properties.instanceId(), now);
	}

	private void resetInventory(Instant now) {
		jdbcTemplate.update("delete from stock_movements");
		jdbcTemplate.update("delete from stock_entries");
		jdbcTemplate.update("delete from products");

		LocalDate today = businessDateProvider.defaultDate();
		seedEntry("Iogurte Natural", 6, today.minusDays(2), now);
		seedEntry("Leite Integral", 12, today.plusDays(4), now);
		seedEntry("Pão de Forma", 8, today.plusDays(18), now);
		seedEntry("Arroz Integral", 4, today.plusDays(60), now);
	}

	private void seedEntry(String name, int quantity, LocalDate expirationDate, Instant now) {
		OffsetDateTime createdAt = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
		Long productId = jdbcTemplate.queryForObject("""
				insert into products (name, normalized_name, created_at)
				values (?, ?, ?)
				returning id
				""", Long.class, name, ProductNameNormalizer.legacyNormalizedName(name), createdAt);
		Long entryId = jdbcTemplate.queryForObject("""
				insert into stock_entries
				    (product_id, initial_quantity, available_quantity, expiration_date, created_at)
				values (?, ?, ?, ?, ?)
				returning id
				""", Long.class, productId, quantity, quantity, expirationDate, createdAt);
		jdbcTemplate.update("""
				insert into stock_movements
				    (stock_entry_id, movement_type, quantity, reason, created_at)
				values (?, 'ENTRY', ?, null, ?)
				""", entryId, quantity, createdAt);
	}

	private Optional<String> metadata(String key) {
		List<String> values = jdbcTemplate.query(
				"select metadata_value from application_metadata where metadata_key = ?",
				(resultSet, rowNumber) -> resultSet.getString(1), key);
		return values.stream().findFirst();
	}

	private void upsertMetadata(String key, String value, Instant updatedAt) {
		jdbcTemplate.update("""
				insert into application_metadata (metadata_key, metadata_value, updated_at)
				values (?, ?, ?)
				on conflict (metadata_key) do update
				set metadata_value = excluded.metadata_value,
				    updated_at = excluded.updated_at
				""", key, value, OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
	}
}
