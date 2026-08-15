package io.github.vitaa1.vencefacil.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.github.vitaa1.vencefacil.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password",
		"app.security.rate-limit.max-failures=2",
		"app.security.rate-limit.max-failures-per-ip=3",
		"app.security.rate-limit.window=15m",
		"app.security.rate-limit.max-keys=100",
		"app.security.rate-limit.trusted-proxy-header=CF-Connecting-IP"
})
class AuthenticationRateLimitTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void blocksRepeatedFailuresWithoutRevealingWhetherTheIdentityExists() throws Exception {
		failedLogin("unknown-operator", "203.0.113.10");
		failedLogin("unknown-operator", "203.0.113.10");

		mockMvc.perform(get("/api/v1/auth/me")
				.with(httpBasic("unknown-operator", "wrong-password"))
				.header("CF-Connecting-IP", "203.0.113.10"))
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.status").value(429));
	}

	@Test
	void keepsDifferentClientAddressesIsolated() throws Exception {
		failedLogin("test-operator", "203.0.113.20");
		failedLogin("test-operator", "203.0.113.20");

		mockMvc.perform(get("/api/v1/auth/me")
				.with(httpBasic("test-operator", "test-password"))
				.header("CF-Connecting-IP", "203.0.113.21"))
			.andExpect(status().isOk());
	}

	@Test
	void blocksClientsThatRotateIdentitiesToBypassTheAccountLimit() throws Exception {
		failedLogin("rotating-operator-1", "203.0.113.40");
		failedLogin("rotating-operator-2", "203.0.113.40");
		failedLogin("rotating-operator-3", "203.0.113.40");

		mockMvc.perform(get("/api/v1/auth/me")
				.with(httpBasic("rotating-operator-4", "wrong-password"))
				.header("CF-Connecting-IP", "203.0.113.40"))
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.status").value(429));
	}

	@Test
	void groupsOversizedIdentitiesIntoABoundedRateLimitKey() throws Exception {
		String sharedPrefix = "x".repeat(121);
		failedLogin(sharedPrefix + "-1", "203.0.113.50");
		failedLogin(sharedPrefix + "-2", "203.0.113.50");

		mockMvc.perform(get("/api/v1/auth/me")
				.with(httpBasic(sharedPrefix + "-3", "wrong-password"))
				.header("CF-Connecting-IP", "203.0.113.50"))
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.status").value(429));
	}

	@Test
	void limitsBasicVariantsAcceptedBySpringSecurity() throws Exception {
		assertBlockedAfterTwoFailures(basicHeader(':', ":wrong-password"), "203.0.113.60");
		assertBlockedAfterTwoFailures(basicHeader('X', "unknown-operator:wrong-password"), "203.0.113.61");
	}

	@Test
	void limitsBasicAuthenticationOutsideTheApiNamespace() throws Exception {
		assertBlockedAfterTwoFailures(
				basicHeader(' ', "unknown-operator:wrong-password"), "203.0.113.62", "/actuator/info");
	}

	@Test
	void doesNotTrustForwardingChainsOrHostnames() throws Exception {
		failedLogin("another-operator", "203.0.113.30, 198.51.100.2");
		failedLogin("another-operator", "attacker.example");
	}

	private void failedLogin(String username, String forwardedAddress) throws Exception {
		mockMvc.perform(get("/api/v1/auth/me")
				.with(httpBasic(username, "wrong-password"))
				.header("CF-Connecting-IP", forwardedAddress))
			.andExpect(status().isUnauthorized());
	}

	private void assertBlockedAfterTwoFailures(String authorization, String forwardedAddress) throws Exception {
		assertBlockedAfterTwoFailures(authorization, forwardedAddress, "/api/v1/auth/me");
	}

	private void assertBlockedAfterTwoFailures(String authorization, String forwardedAddress, String path)
			throws Exception {
		for (int attempt = 0; attempt < 2; attempt++) {
			mockMvc.perform(get(path)
					.header("Authorization", authorization)
					.header("CF-Connecting-IP", forwardedAddress))
				.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(get(path)
				.header("Authorization", authorization)
				.header("CF-Connecting-IP", forwardedAddress))
			.andExpect(status().isTooManyRequests());
	}

	private String basicHeader(char separator, String credentials) {
		return "Basic" + separator + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}
}
