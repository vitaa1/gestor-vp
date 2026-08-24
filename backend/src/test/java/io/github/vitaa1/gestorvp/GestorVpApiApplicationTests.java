package io.github.vitaa1.gestorvp;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password"
})
class GestorVpApiApplicationTests {
	@Autowired
	private HikariDataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void disablesNamedServerPreparedStatementsForParameterSensitiveQueries() {
		assertThat(dataSource.getDataSourceProperties().getProperty("prepareThreshold")).isEqualTo("0");
	}

}
