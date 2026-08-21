package io.github.vitaa1.gestorvp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties("app.security")
record ApplicationSecurityProperties(
		@NotBlank @Size(max = 120) String username,
		@NotBlank @Size(min = 12, max = 200) String password) {
}
