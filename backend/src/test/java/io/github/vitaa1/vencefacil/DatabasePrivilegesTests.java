package io.github.vitaa1.vencefacil;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password",
		"spring.flyway.user=vence_facil_migration",
		"spring.flyway.password=migration-test-password"
})
class DatabasePrivilegesTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PostgreSQLContainer postgresContainer;

	@Test
	void runtimeCanUseApplicationObjectsButCannotChangeSchemaOrFlywayHistory() throws SQLException {
		assertThat(hasTablePrivilege("products", "SELECT")).isTrue();
		assertThat(hasTablePrivilege("stock_entries", "INSERT")).isTrue();
		assertThat(hasTablePrivilege("stock_movements", "SELECT")).isTrue();
		assertThat(hasSequencePrivilege("products_id_seq", "USAGE")).isTrue();
		assertThat(hasSchemaPrivilege("CREATE")).isFalse();
		assertThat(hasTablePrivilege("flyway_schema_history", "SELECT")).isFalse();
		assertThat(hasTablePrivilege("flyway_schema_history", "INSERT")).isFalse();
		assertThat(hasTablePrivilege("flyway_schema_history", "UPDATE")).isFalse();
		assertThat(hasTablePrivilege("flyway_schema_history", "DELETE")).isFalse();

		try (var connection = DriverManager.getConnection(
				postgresContainer.getJdbcUrl(), "vence_facil_runtime", "runtime-test-password");
				var statement = connection.prepareStatement(
						"insert into products (name, normalized_name, created_at) values (?, ?, current_timestamp)")) {
			statement.setString(1, "Produto de teste");
			statement.setString(2, "produto de teste");
			assertThat(statement.executeUpdate()).isEqualTo(1);
		}
	}

	@Test
	void productSearchMigrationSupportsLegacyWritesAfterTheUpgrade() throws SQLException {
		String schema = "product_name_migration_test";
		try (var connection = DriverManager.getConnection(
				postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword());
				var statement = connection.createStatement()) {
			statement.execute("create schema " + schema);
		}

		try {
			Flyway.configure()
				.dataSource(postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword())
				.schemas(schema)
				.defaultSchema(schema)
				.target(MigrationVersion.fromVersion("4"))
				.load()
				.migrate();

			try (var connection = DriverManager.getConnection(
					postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword());
					var statement = connection.createStatement()) {
				statement.execute("set search_path to " + schema);
				statement.executeUpdate("""
						insert into products (id, name, normalized_name, created_at)
						values (1, 'Pão de Forma', 'pão de forma', current_timestamp)
						""");
				statement.executeUpdate("""
						insert into stock_entries
						    (product_id, initial_quantity, available_quantity, expiration_date, created_at)
						values (1, 1, 1, current_date + 1, current_timestamp)
						""");
			}

			Flyway.configure()
				.dataSource(postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword())
				.schemas(schema)
				.defaultSchema(schema)
				.load()
				.migrate();

			try (var connection = DriverManager.getConnection(
					postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword());
					var statement = connection.createStatement()) {
				statement.execute("set search_path to " + schema);
				statement.executeUpdate("""
						insert into products (id, name, normalized_name, created_at)
						values (2, U&'Cafe\\0301 com Leite', U&'cafe\\0301 com leite', current_timestamp)
						""");
				try (var result = statement.executeQuery("""
						select (select count(*) from products),
						       (select count(*) from stock_entries),
						       (select string_agg(search_name, ',' order by id) from products),
						       (select normalized_name from products where id = 1)
						""")) {
					assertThat(result.next()).isTrue();
					assertThat(result.getLong(1)).isEqualTo(2);
					assertThat(result.getLong(2)).isEqualTo(1);
					assertThat(result.getString(3)).isEqualTo("pao de forma,cafe com leite");
					assertThat(result.getString(4)).isEqualTo("pão de forma");
				}
			}
		}
		finally {
			try (var connection = DriverManager.getConnection(
					postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword());
					var statement = connection.createStatement()) {
				statement.execute("drop schema " + schema + " cascade");
			}
		}
	}

	private boolean hasTablePrivilege(String table, String privilege) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
				"select has_table_privilege('vence_facil_runtime', ?, ?)",
				Boolean.class,
				"public." + table,
				privilege));
	}

	private boolean hasSequencePrivilege(String sequence, String privilege) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
				"select has_sequence_privilege('vence_facil_runtime', ?, ?)",
				Boolean.class,
				"public." + sequence,
				privilege));
	}

	private boolean hasSchemaPrivilege(String privilege) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
				"select has_schema_privilege('vence_facil_runtime', 'public', ?)",
				Boolean.class,
				privilege));
	}
}
