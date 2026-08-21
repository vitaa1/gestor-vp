package io.github.vitaa1.vencefacil.inventory;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class InventoryConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	BusinessDateProvider businessDateProvider(Clock clock,
			@Value("${app.inventory.default-time-zone}") String defaultTimeZone) {
		return new BusinessDateProvider(clock, defaultTimeZone);
	}
}
