package io.github.vitaa1.vencefacil.security;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController {

	@GetMapping("/me")
	AuthenticatedUser currentUser(Principal principal) {
		return new AuthenticatedUser(principal.getName());
	}

	record AuthenticatedUser(String username) {
	}
}
