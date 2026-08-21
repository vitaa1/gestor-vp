package io.github.vitaa1.vencefacil.inventory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import io.github.vitaa1.vencefacil.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, StockEntryControllerTests.FixedTimeConfiguration.class })
@AutoConfigureMockMvc
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password",
		"app.inventory.default-time-zone=UTC"
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

		mockMvc.perform(get("/api/v1/stock-entries?cursorId=10").with(operator()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void keepsActiveStockPaginationStableWhenEntriesChangeBetweenSlices() throws Exception {
		long firstEntryId = createdEntryId("Produto 1", 1, "2030-01-01");
		long secondEntryId = createdEntryId("Produto 2", 1, "2030-01-02");
		long thirdEntryId = createdEntryId("Produto 3", 1, "2030-01-02");

		MvcResult firstSlice = mockMvc.perform(get("/api/v1/stock-entries?size=2").with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].id").value(firstEntryId))
			.andExpect(jsonPath("$.content[1].id").value(secondEntryId))
			.andExpect(jsonPath("$.hasNext").value(true))
			.andExpect(jsonPath("$.nextCursorExpirationDate").value("2030-01-02"))
			.andExpect(jsonPath("$.nextCursorCreatedAt").isString())
			.andExpect(jsonPath("$.nextCursorId").value(secondEntryId))
			.andReturn();
		String cursorExpirationDate = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorExpirationDate");
		String cursorCreatedAt = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorCreatedAt");
		Number cursorId = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorId");

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", firstEntryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":1,"reason":"USED"}
						"""))
			.andExpect(status().isOk());
		createdEntryId("Produto anterior", 1, "2029-12-31");

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("size", "2")
				.param("cursorExpirationDate", cursorExpirationDate)
				.param("cursorCreatedAt", cursorCreatedAt)
				.param("cursorId", cursorId.toString())
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].id").value(thirdEntryId))
			.andExpect(jsonPath("$.hasNext").value(false))
			.andExpect(jsonPath("$.nextCursorExpirationDate").doesNotExist())
			.andExpect(jsonPath("$.nextCursorCreatedAt").doesNotExist())
			.andExpect(jsonPath("$.nextCursorId").doesNotExist());
	}

	@Test
	void finishesActiveStockPaginationWhenTheRemainingEntryIsClosedBetweenSlices() throws Exception {
		createdEntryId("Produto 1", 1, "2030-01-01");
		long remainingEntryId = createdEntryId("Produto 2", 1, "2030-01-02");

		MvcResult firstSlice = mockMvc.perform(get("/api/v1/stock-entries?size=1").with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hasNext").value(true))
			.andReturn();
		String cursorExpirationDate = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorExpirationDate");
		String cursorCreatedAt = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorCreatedAt");
		Number cursorId = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorId");

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", remainingEntryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":1,"reason":"USED"}
						"""))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("size", "1")
				.param("cursorExpirationDate", cursorExpirationDate)
				.param("cursorCreatedAt", cursorCreatedAt)
				.param("cursorId", cursorId.toString())
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isEmpty())
			.andExpect(jsonPath("$.hasNext").value(false))
			.andExpect(jsonPath("$.nextCursorExpirationDate").doesNotExist())
			.andExpect(jsonPath("$.nextCursorCreatedAt").doesNotExist())
			.andExpect(jsonPath("$.nextCursorId").doesNotExist());
	}

	@Test
	void reusesProductIgnoringCaseAndExtraWhitespace() throws Exception {
		createEntry("Leite Integral", 5, "2028-01-01").andExpect(status().isCreated());
		createEntry("  leite   integral  ", 3, "2028-02-01").andExpect(status().isCreated());

		assertThatSingleProductWasCreated();
		org.assertj.core.api.Assertions.assertThat(stockMovementRepository.count()).isEqualTo(2);
	}

	@Test
	void showsEntryDetailsWithInitialAndAvailableQuantities() throws Exception {
		long entryId = createdEntryId("Leite Integral", 12, "2030-01-10");

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId).with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(entryId))
			.andExpect(jsonPath("$.productName").value("Leite Integral"))
			.andExpect(jsonPath("$.initialQuantity").value(12))
			.andExpect(jsonPath("$.availableQuantity").value(12))
			.andExpect(jsonPath("$.expirationDate").value("2030-01-10"))
			.andExpect(jsonPath("$.statusLabel").isString());
	}

	@Test
	void withdrawsUnitsAtomicallyAndRecordsTheReason() throws Exception {
		long entryId = createdEntryId("Leite Integral", 12, "2030-01-10");

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":5,"reason":"SOLD"}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.initialQuantity").value(12))
			.andExpect(jsonPath("$.availableQuantity").value(7));

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId).with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableQuantity").value(7));

		org.assertj.core.api.Assertions.assertThat(stockMovementRepository.findAll())
			.hasSize(2)
			.anySatisfy(movement -> {
				org.assertj.core.api.Assertions.assertThat(movement.getMovementType())
					.isEqualTo(MovementType.WITHDRAWAL);
				org.assertj.core.api.Assertions.assertThat(movement.getQuantity()).isEqualTo(5);
				org.assertj.core.api.Assertions.assertThat(movement.getReason()).isEqualTo("SOLD");
			});
	}

	@Test
	void rejectsAWithdrawalAboveTheAvailableQuantityWithoutChangingTheBalance() throws Exception {
		long entryId = createdEntryId("Arroz Integral", 4, "2030-10-20");

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":5,"reason":"USED"}
						"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail").value("A quantidade informada supera o saldo disponível."));

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId).with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableQuantity").value(4));
		org.assertj.core.api.Assertions.assertThat(stockMovementRepository.count()).isEqualTo(1);
	}

	@Test
	void allowsOnlyLossOrExpirationReasonsForAnExpiredEntry() throws Exception {
		long entryId = createdEntryId("Iogurte Natural", 6, "2020-01-01");

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":1,"reason":"SOLD"}
						"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.detail")
				.value("Entradas vencidas aceitam somente os motivos Perdi ou Venceu."));

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":2,"reason":"EXPIRED"}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableQuantity").value(4));
	}

	@Test
	void usesTheOperatorsTimeZoneForStatusWithoutChangingTrustedWithdrawalRules() throws Exception {
		long entryId = createdEntryId("Bolo de Milho", 3, "2026-08-21", "America/Sao_Paulo");

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId)
				.with(operator())
				.header("X-User-Time-Zone", "America/Sao_Paulo"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ATTENTION"))
			.andExpect(jsonPath("$.daysUntilExpiration").value(0));

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId)
				.with(operator())
				.header("X-User-Time-Zone", "UTC"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("EXPIRED"))
			.andExpect(jsonPath("$.daysUntilExpiration").value(-1));

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
				.with(operator())
				.header("X-User-Time-Zone", "America/Sao_Paulo")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":1,"reason":"SOLD"}
						"""))
			.andExpect(status().isUnprocessableEntity());

		org.assertj.core.api.Assertions.assertThat(stockMovementRepository.count()).isEqualTo(1);
	}

	@Test
	void rejectsAnInvalidUserTimeZone() throws Exception {
		mockMvc.perform(get("/api/v1/stock-entries")
				.with(operator())
				.header("X-User-Time-Zone", "Mars/Olympus"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("O fuso horário informado é inválido."));
	}

	@Test
	void rejectsInvalidWithdrawalDataAndUnknownEntries() throws Exception {
		long entryId = createdEntryId("Pão de Forma", 8, "2030-01-01");

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":0,"reason":null}
						"""))
			.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", 999_999).with(operator()))
			.andExpect(status().isNotFound());
	}

	@Test
	void removesAnEntryFromActiveStockWhenTheBalanceReachesZero() throws Exception {
		long entryId = createdEntryId("Arroz Integral", 4, "2030-10-20");

		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":4,"reason":"USED"}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableQuantity").value(0));

		mockMvc.perform(get("/api/v1/stock-entries").with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(0)));

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId).with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableQuantity").value(0));
	}

	@Test
	void preventsConcurrentWithdrawalsFromProducingANegativeBalance() throws Exception {
		long entryId = createdEntryId("Pão de Forma", 5, "2030-01-01");

		CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> withdrawalStatus(entryId, 4));
		CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> withdrawalStatus(entryId, 4));
		List<Integer> statuses = java.util.stream.Stream.of(first.join(), second.join()).sorted().toList();

		org.assertj.core.api.Assertions.assertThat(statuses).containsExactly(200, 422);
		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId).with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableQuantity").value(1));
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

	private long createdEntryId(String productName, int quantity, String expirationDate) throws Exception {
		String response = createEntry(productName, quantity, expirationDate)
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return com.jayway.jsonpath.JsonPath.<Integer>read(response, "$.id").longValue();
	}

	private long createdEntryId(String productName, int quantity, String expirationDate, String timeZone)
			throws Exception {
		String response = mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
				.header("X-User-Time-Zone", timeZone)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"%s","quantity":%d,"expirationDate":"%s"}
						""".formatted(productName, quantity, expirationDate)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return com.jayway.jsonpath.JsonPath.<Integer>read(response, "$.id").longValue();
	}

	private int withdrawalStatus(long entryId, int quantity) {
		try {
			return mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
					.with(operator())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"quantity":%d,"reason":"SOLD"}
							""".formatted(quantity)))
				.andReturn()
				.getResponse()
				.getStatus();
		}
		catch (Exception exception) {
			throw new CompletionException(exception);
		}
	}

	private void assertThatSingleProductWasCreated() {
		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(1);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedTimeConfiguration {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-08-22T00:30:00Z"), ZoneOffset.UTC);
		}
	}
}
