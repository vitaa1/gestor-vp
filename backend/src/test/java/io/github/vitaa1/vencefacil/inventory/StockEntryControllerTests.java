package io.github.vitaa1.vencefacil.inventory;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import io.github.vitaa1.vencefacil.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password"
})
class StockEntryControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockMovementRepository stockMovementRepository;

	@Autowired
	private StockEntryRepository stockEntryRepository;

	@Autowired
	private ProductRepository productRepository;

	@BeforeEach
	void cleanDatabase() {
		stockMovementRepository.deleteAll();
		stockEntryRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void createsAnEntryAndListsActiveStockByExpirationDate() throws Exception {
		createEntry("Arroz Integral", 4, "2030-10-20")
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Location"))
			.andExpect(jsonPath("$.productName").value("Arroz Integral"))
			.andExpect(jsonPath("$.quantity").value(4));

		createEntry("Leite Integral", 12, "2029-01-10")
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/stock-entries").with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].productName").value("Leite Integral"))
			.andExpect(jsonPath("$.content[1].productName").value("Arroz Integral"));
	}

	@Test
	void rejectsInvalidEntry() throws Exception {
		mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":" ","quantity":0,"expirationDate":null}
						"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void requiresValidCredentialsForInventory() throws Exception {
		mockMvc.perform(get("/api/v1/stock-entries"))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/stock-entries")
				.with(httpBasic("test-operator", "wrong-password"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"Leite","quantity":1,"expirationDate":"2028-01-01"}
						"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void limitsTheRequestedPageSize() throws Exception {
		mockMvc.perform(get("/api/v1/stock-entries?size=101").with(operator()))
			.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/stock-entries?page=10001").with(operator()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void reusesProductIgnoringCaseAndExtraWhitespace() throws Exception {
		createEntry("Leite Integral", 5, "2028-01-01").andExpect(status().isCreated());
		createEntry("  leite   integral  ", 3, "2028-02-01").andExpect(status().isCreated());

		assertThatSingleProductWasCreated();
		org.assertj.core.api.Assertions.assertThat(stockMovementRepository.count()).isEqualTo(2);
	}

	private org.springframework.test.web.servlet.ResultActions createEntry(String productName, int quantity,
			String expirationDate) throws Exception {
		return mockMvc.perform(post("/api/v1/stock-entries")
			.with(operator())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"productName":"%s","quantity":%d,"expirationDate":"%s"}
					""".formatted(productName, quantity, expirationDate)));
	}

	private RequestPostProcessor operator() {
		return httpBasic("test-operator", "test-password");
	}

	private void assertThatSingleProductWasCreated() {
		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(1);
	}
}
