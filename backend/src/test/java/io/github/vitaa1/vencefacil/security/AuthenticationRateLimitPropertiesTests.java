package io.github.vitaa1.vencefacil.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AuthenticationRateLimitPropertiesTests {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void rejectsANonPositiveWindow() {
		AuthenticationRateLimitProperties properties = new AuthenticationRateLimitProperties(
				2, 3, Duration.ZERO, 100, "X-Real-IP");

		assertThat(validator.validate(properties))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("windowPositive");
	}
}
