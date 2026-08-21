package io.github.vitaa1.gestorvp.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;

@Validated
@ConfigurationProperties("app.security.rate-limit")
record AuthenticationRateLimitProperties(
		@Min(1) @Max(100) int maxFailures,
		@Min(1) @Max(1_000) int maxFailuresPerIp,
		@NotNull Duration window,
		@Min(100) @Max(100_000) int maxKeys,
		@Pattern(regexp = "[A-Za-z0-9-]{0,80}") String trustedProxyHeader) {

	@AssertTrue(message = "must be positive")
	boolean isWindowPositive() {
		return window == null || (!window.isZero() && !window.isNegative());
	}
}
