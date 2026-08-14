package io.github.vitaa1.vencefacil.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApplicationSecurityProperties.class)
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable());

		return http.build();
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
