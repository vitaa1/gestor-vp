package io.github.vitaa1.vencefacil;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
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
