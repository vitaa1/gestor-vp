package io.github.vitaa1.vencefacil;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password"
})
class VenceFacilApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
