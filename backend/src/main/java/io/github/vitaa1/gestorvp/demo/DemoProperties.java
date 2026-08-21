package io.github.vitaa1.gestorvp.demo;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties("app.demo")
record DemoProperties(
		boolean enabled,
		@Size(max = 120) String instanceId,
		@NotNull Duration resetAfter) {

	@AssertTrue(message = "DEMO_INSTANCE_ID is required when DEMO_MODE is enabled")
	boolean hasSafeConfiguration() {
		return !enabled || StringUtils.hasText(instanceId);
	}

	@AssertTrue(message = "DEMO_RESET_AFTER must be greater than zero")
	boolean hasPositiveResetInterval() {
		return resetAfter != null && !resetAfter.isZero() && !resetAfter.isNegative();
	}
}
