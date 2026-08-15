package io.github.vitaa1.vencefacil.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.vitaa1.vencefacil.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, DemoDataServiceTests.MutableTimeConfiguration.class })
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password",
		"app.demo.enabled=true",
		"app.demo.instance-id=demo-data-service-tests",
		"app.demo.reset-after=24h"
})
class DemoDataServiceTests {

	@Autowired
	private DemoDataService demoDataService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MutableClock clock;

	@BeforeEach
	void restoreKnownDemoDatabase() {
		jdbcTemplate.update("delete from stock_movements");
		jdbcTemplate.update("delete from stock_entries");
		jdbcTemplate.update("delete from products");
		jdbcTemplate.update("delete from application_metadata");
		clock.advance(Duration.ofHours(25));
		demoDataService.resetIfDue();
	}

	@Test
	void initializesAnEmptyDedicatedDatabaseWithRepresentativeData() {
		assertThat(count("products")).isEqualTo(4);
		assertThat(count("stock_entries")).isEqualTo(4);
		assertThat(count("stock_movements")).isEqualTo(4);
		assertThat(metadata("demo_instance_id")).isEqualTo("demo-data-service-tests");
	}

	@Test
	void preservesChangesBeforeTheDailyResetIsDue() {
		insertExtraProduct();

		demoDataService.resetIfDue();

		assertThat(count("products")).isEqualTo(5);
	}

	@Test
	void replacesDemoDataAfterTheDailyResetIsDue() {
		insertExtraProduct();
		clock.advance(Duration.ofHours(25));

		demoDataService.resetIfDue();

		assertThat(count("products")).isEqualTo(4);
		assertThat(productCount("Produto temporário")).isZero();
	}

	@Test
	void refusesToDeleteDataWhenTheInstanceMarkerDoesNotMatch() {
		jdbcTemplate.update("update application_metadata set metadata_value = 'another-instance' where metadata_key = 'demo_instance_id'");
		clock.advance(Duration.ofHours(25));

		assertThatThrownBy(demoDataService::resetIfDue)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("does not match");
		assertThat(count("products")).isEqualTo(4);
	}

	@Test
	void refusesToMarkANonEmptyDatabaseAsDemo() {
		jdbcTemplate.update("delete from application_metadata");
		clock.advance(Duration.ofHours(25));

		assertThatThrownBy(demoDataService::resetIfDue)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("empty, dedicated database");
		assertThat(count("products")).isEqualTo(4);
	}

	private void insertExtraProduct() {
		jdbcTemplate.update("insert into products (name, normalized_name, created_at) values (?, ?, ?)",
				"Produto temporário", "produto temporário", OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
	}

	private long productCount(String name) {
		return jdbcTemplate.queryForObject("select count(*) from products where name = ?", Long.class, name);
	}

	private String metadata(String key) {
		return jdbcTemplate.queryForObject(
				"select metadata_value from application_metadata where metadata_key = ?", String.class, key);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class MutableTimeConfiguration {

		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(Instant.parse("2026-08-14T12:00:00Z"));
		}
	}

	static final class MutableClock extends Clock {

		private final AtomicReference<Instant> instant;

		MutableClock(Instant initialInstant) {
			this.instant = new AtomicReference<>(initialInstant);
		}

		void advance(Duration duration) {
			instant.updateAndGet(current -> current.plus(duration));
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant.get();
		}
	}
}
