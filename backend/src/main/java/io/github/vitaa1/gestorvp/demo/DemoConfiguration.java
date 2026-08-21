package io.github.vitaa1.gestorvp.demo;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DemoProperties.class)
class DemoConfiguration {

	@Bean
	ApplicationRunner demoDataInitializer(DemoProperties properties, DemoDataService demoDataService) {
		return arguments -> {
			if (properties.enabled()) {
				demoDataService.resetIfDue();
			}
		};
	}
}
