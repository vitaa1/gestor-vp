package io.github.vitaa1.gestorvp.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.github.vitaa1.gestorvp.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password"
})
class WebBoundaryTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void servesTheSpaWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(forwardedUrl("index.html"));

		mockMvc.perform(get("/products/active"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("test-spa")));
	}

	@Test
	void neverUsesTheSpaFallbackForApiOrActuatorPaths() throws Exception {
		mockMvc.perform(get("/api/v1/missing"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().doesNotExist("WWW-Authenticate"));

		mockMvc.perform(get("/api/v1/missing").with(httpBasic("test-operator", "wrong-password")))
			.andExpect(status().isUnauthorized())
			.andExpect(header().doesNotExist("WWW-Authenticate"));

		mockMvc.perform(get("/api/v1/missing").with(httpBasic("test-operator", "test-password")))
			.andExpect(status().isNotFound());

		mockMvc.perform(get("/actuator/info"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string("WWW-Authenticate", containsString("Basic")));

		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk());
	}

	@Test
	void neverServesClasspathResourcesOutsideTheStaticDirectory() throws Exception {
		mockMvc.perform(get("/../application.properties"))
			.andExpect(status().is4xxClientError());

		mockMvc.perform(get("/%2e%2e/application.properties"))
			.andExpect(status().is4xxClientError());
	}
}
