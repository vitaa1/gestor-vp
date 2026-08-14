package io.github.vitaa1.vencefacil.inventory;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class InventoryConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}
}
