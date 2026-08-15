package io.github.vitaa1.vencefacil.demo;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
class DemoResetFilter extends OncePerRequestFilter {

	private final DemoDataService demoDataService;
	private final DemoProperties properties;

	DemoResetFilter(DemoDataService demoDataService, DemoProperties properties) {
		this.demoDataService = demoDataService;
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		demoDataService.resetIfDue();
		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !properties.enabled();
	}
}
