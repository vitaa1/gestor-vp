package io.github.vitaa1.vencefacil.security;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ ApplicationSecurityProperties.class, AuthenticationRateLimitProperties.class })
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			AuthenticationRateLimitFilter authenticationRateLimitFilter) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
				.requestMatchers("/api/**", "/actuator/**").authenticated()
				.requestMatchers(HttpMethod.GET, "/**").permitAll()
				.anyRequest().denyAll())
			.httpBasic(httpBasic -> httpBasic
				.authenticationEntryPoint(authenticationEntryPoint()))
			.addFilterBefore(authenticationRateLimitFilter, BasicAuthenticationFilter.class)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable());

		return http.build();
	}

	private AuthenticationEntryPoint authenticationEntryPoint() {
		BasicAuthenticationEntryPoint actuatorEntryPoint = new BasicAuthenticationEntryPoint();
		actuatorEntryPoint.setRealmName("Vence Facil");

		return (request, response, exception) -> {
			if (request.getRequestURI().startsWith("/actuator/")) {
				actuatorEntryPoint.commence(request, response, exception);
				return;
			}
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		};
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(ApplicationSecurityProperties properties, PasswordEncoder passwordEncoder) {
		return new InMemoryUserDetailsManager(User.withUsername(properties.username())
			.password(passwordEncoder.encode(properties.password()))
			.roles("OPERATOR")
			.build());
	}
}
