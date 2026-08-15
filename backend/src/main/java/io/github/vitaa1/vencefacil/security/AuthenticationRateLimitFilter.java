package io.github.vitaa1.vencefacil.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
class AuthenticationRateLimitFilter extends OncePerRequestFilter {

	private static final int MAX_IDENTITY_LENGTH = 120;
	private static final String UNPARSEABLE_IDENTITY = "<unparseable>";
	private static final String OVERSIZED_IDENTITY = "<oversized>";
	private static final BasicAuthenticationConverter BASIC_AUTHENTICATION_CONVERTER =
			new BasicAuthenticationConverter();

	private final ClientIpResolver clientIpResolver;
	private final AuthenticationRateLimiter rateLimiter;

	AuthenticationRateLimitFilter(ClientIpResolver clientIpResolver, AuthenticationRateLimiter rateLimiter) {
		this.clientIpResolver = clientIpResolver;
		this.rateLimiter = rateLimiter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Optional<String> identity = basicIdentity(request);
		if (identity.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientIp = clientIpResolver.resolve(request);
		if (rateLimiter.isBlocked(identity.get(), clientIp)) {
			writeTooManyRequests(response);
			return;
		}

		filterChain.doFilter(request, response);
		if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
			rateLimiter.recordFailure(identity.get(), clientIp);
		}
		else {
			rateLimiter.recordSuccess(identity.get(), clientIp);
		}
	}

	private Optional<String> basicIdentity(HttpServletRequest request) {
		try {
			var authentication = BASIC_AUTHENTICATION_CONVERTER.convert(request);
			if (authentication == null) {
				return Optional.empty();
			}
			String identity = authentication.getName();
			if (identity.isEmpty()) {
				return Optional.of(UNPARSEABLE_IDENTITY);
			}
			return Optional.of(identity.length() <= MAX_IDENTITY_LENGTH ? identity : OVERSIZED_IDENTITY);
		}
		catch (BadCredentialsException exception) {
			return Optional.of(UNPARSEABLE_IDENTITY);
		}
	}

	private void writeTooManyRequests(HttpServletResponse response) throws IOException {
		response.setStatus(429);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write("{\"status\":429,\"title\":\"Muitas tentativas. Tente novamente mais tarde.\"}");
	}
}
