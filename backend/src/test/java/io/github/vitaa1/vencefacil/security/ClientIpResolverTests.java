package io.github.vitaa1.vencefacil.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTests {

	private final ClientIpResolver resolver = new ClientIpResolver(new AuthenticationRateLimitProperties(
			2, 3, Duration.ofMinutes(15), 100, "X-Real-IP"));

	@Test
	void acceptsASingleNumericAddressFromTheConfiguredProxyHeader() {
		MockHttpServletRequest request = requestFrom("192.0.2.10");
		request.addHeader("X-Real-IP", "203.0.113.10");

		assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
	}

	@Test
	void fallsBackToTheConnectionAddressForChainsAndHostnames() {
		MockHttpServletRequest chain = requestFrom("192.0.2.20");
		chain.addHeader("X-Real-IP", "203.0.113.10, 198.51.100.2");
		MockHttpServletRequest hostname = requestFrom("192.0.2.21");
		hostname.addHeader("X-Real-IP", "attacker.example");
		MockHttpServletRequest hexadecimalHostname = requestFrom("192.0.2.22");
		hexadecimalHostname.addHeader("X-Real-IP", "face.de");

		assertThat(resolver.resolve(chain)).isEqualTo("192.0.2.20");
		assertThat(resolver.resolve(hostname)).isEqualTo("192.0.2.21");
		assertThat(resolver.resolve(hexadecimalHostname)).isEqualTo("192.0.2.22");
	}

	@Test
	void acceptsValidIpv6Literals() {
		assertThat(resolveForwarded("2001:db8:0:1:1:1:1:1")).isEqualTo("2001:db8:0:1:1:1:1:1");
		assertThat(resolveForwarded("2001:db8::1")).isEqualTo("2001:db8::1");
		assertThat(resolveForwarded("::ffff:192.0.2.128")).isEqualTo("::ffff:192.0.2.128");
	}

	@Test
	void rejectsMalformedIpv6Literals() {
		assertThat(resolveForwarded("2001:db8::1::2")).isEqualTo("192.0.2.30");
		assertThat(resolveForwarded("1:2:3:4:5:6:7:8:9")).isEqualTo("192.0.2.30");
		assertThat(resolveForwarded("12345::1")).isEqualTo("192.0.2.30");
		assertThat(resolveForwarded("192.0.2.1::")).isEqualTo("192.0.2.30");
	}

	private String resolveForwarded(String address) {
		MockHttpServletRequest request = requestFrom("192.0.2.30");
		request.addHeader("X-Real-IP", address);
		return resolver.resolve(request);
	}

	private MockHttpServletRequest requestFrom(String remoteAddress) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(remoteAddress);
		return request;
	}
}
