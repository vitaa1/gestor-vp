package io.github.vitaa1.gestorvp.inventory;

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

import io.github.vitaa1.gestorvp.TestcontainersConfiguration;

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
	void createsAnEntryWithOptionalProductAndStockDetails() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "productName":"Café Especial",
						  "quantity":6,
						  "expirationDate":"2030-04-15",
						  "barcode":"07891234567890",
						  "category":" Mercearia ",
						  "unitCost":18.75,
						  "supplier":" Torrefação Central ",
						  "batchNumber":" LOTE-2030-A "
						}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.barcode").value("07891234567890"))
			.andExpect(jsonPath("$.category").value("Mercearia"))
			.andExpect(jsonPath("$.unitCost").value(18.75))
			.andExpect(jsonPath("$.supplier").value("Torrefação Central"))
			.andExpect(jsonPath("$.batchNumber").value("LOTE-2030-A"))
			.andReturn();
		Number entryId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId).with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.barcode").value("07891234567890"))
			.andExpect(jsonPath("$.category").value("Mercearia"))
			.andExpect(jsonPath("$.unitCost").value(18.75))
			.andExpect(jsonPath("$.supplier").value("Torrefação Central"))
			.andExpect(jsonPath("$.batchNumber").value("LOTE-2030-A"));
	}

	@Test
	void enrichesAnExistingProductWithoutErasingDetailsWhenTheyAreOmitted() throws Exception {
		createEntry("Aveia em Flocos", 2, "2030-01-10")
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "productName":"Aveia em Flocos",
						  "quantity":3,
						  "expirationDate":"2030-02-10",
						  "barcode":"7891234567890",
						  "category":"Cereais"
						}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.barcode").value("7891234567890"))
			.andExpect(jsonPath("$.category").value("Cereais"));

		createEntry("Aveia em Flocos", 1, "2030-03-10")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.barcode").value("7891234567890"))
			.andExpect(jsonPath("$.category").value("Cereais"));
		assertThatSingleProductWasCreated();
	}

	@Test
	void rejectsAConflictingCategoryForAnExistingProduct() throws Exception {
		entryCreationStatus("Aveia em Flocos", null, "Cereais", "2030-01-10");

		mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"Aveia em Flocos","quantity":1,
						 "expirationDate":"2030-02-10","category":"Mercearia"}
						"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("O produto informado já pertence a outra categoria."));

		org.assertj.core.api.Assertions.assertThat(stockEntryRepository.count()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(
				productRepository.findFirstBySearchNameOrderById("aveia em flocos").orElseThrow().getCategory())
			.isEqualTo("Cereais");
	}

	@Test
	void rejectsInvalidOrDuplicateBarcodes() throws Exception {
		mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"Produto inválido","quantity":1,
						 "expirationDate":"2030-01-01","barcode":"ABC123"}
						"""))
			.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"Produto A","quantity":1,
						 "expirationDate":"2030-01-01","barcode":"7891234567890"}
						"""))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"Produto B","quantity":1,
						 "expirationDate":"2030-02-01","barcode":"7891234567890"}
						"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("O código de barras informado já pertence a outro produto."));

		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(stockEntryRepository.count()).isEqualTo(1);
	}

	@Test
	void atomicallyClaimsTheBarcodeForAnExistingProduct() throws Exception {
		createEntry("Produto legado", 1, "2030-01-01")
			.andExpect(status().isCreated());

		CompletableFuture<Integer> first = CompletableFuture.supplyAsync(
				() -> entryCreationStatus("Produto legado", "7891234567890", "2030-02-01"));
		CompletableFuture<Integer> second = CompletableFuture.supplyAsync(
				() -> entryCreationStatus("Produto legado", "7891234567891", "2030-03-01"));
		List<Integer> statuses = java.util.stream.Stream.of(first.join(), second.join()).sorted().toList();

		org.assertj.core.api.Assertions.assertThat(statuses).containsExactly(201, 409);
		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(stockEntryRepository.count()).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(
				productRepository.findFirstBySearchNameOrderById("produto legado").orElseThrow().getBarcode())
			.isIn("7891234567890", "7891234567891");
	}

	@Test
	void atomicallyClaimsTheCategoryForAnExistingProduct() throws Exception {
		createEntry("Produto legado", 1, "2030-01-01")
			.andExpect(status().isCreated());

		CompletableFuture<Integer> first = CompletableFuture.supplyAsync(
				() -> entryCreationStatus("Produto legado", null, "Cereais", "2030-02-01"));
		CompletableFuture<Integer> second = CompletableFuture.supplyAsync(
				() -> entryCreationStatus("Produto legado", null, "Mercearia", "2030-03-01"));
		List<Integer> statuses = java.util.stream.Stream.of(first.join(), second.join()).sorted().toList();

		org.assertj.core.api.Assertions.assertThat(statuses).containsExactly(201, 409);
		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(stockEntryRepository.count()).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(
				productRepository.findFirstBySearchNameOrderById("produto legado").orElseThrow().getCategory())
			.isIn("Cereais", "Mercearia");
	}

	@Test
	void rejectsConcurrentBarcodeClaimsFromDifferentProducts() {
		CompletableFuture<Integer> first = CompletableFuture.supplyAsync(
				() -> entryCreationStatus("Produto A", "7891234567890", null, "2030-01-01"));
		CompletableFuture<Integer> second = CompletableFuture.supplyAsync(
				() -> entryCreationStatus("Produto B", "7891234567890", null, "2030-02-01"));
		List<Integer> statuses = java.util.stream.Stream.of(first.join(), second.join()).sorted().toList();

		org.assertj.core.api.Assertions.assertThat(statuses).containsExactly(201, 409);
		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(stockEntryRepository.count()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(stockMovementRepository.count()).isEqualTo(1);
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

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("query", "a".repeat(121))
				.with(operator()))
			.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/stock-entries?status=UNKNOWN").with(operator()))
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
	void reusesAndFindsProductsIgnoringCanonicalAccentsCaseAndExtraWhitespace() throws Exception {
		createEntry("Pão de Forma", 5, "2028-01-01").andExpect(status().isCreated());
		createEntry("  PAO   DE forma ", 3, "2028-02-01").andExpect(status().isCreated());
		createEntry("Cafe\u0301 com Leite", 2, "2028-02-15").andExpect(status().isCreated());
		createEntry("Arroz Integral", 4, "2028-03-01").andExpect(status().isCreated());
		org.assertj.core.api.Assertions.assertThat(productRepository.count()).isEqualTo(3);

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("query", "  PAO   DE  ")
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].productName").value("Pão de Forma"))
			.andExpect(jsonPath("$.content[1].productName").value("Pão de Forma"));

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("query", "cafe com leite")
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].productName").value("Café com Leite"));
	}

	@Test
	void combinesProductNameSearchWithExpirationStatusFilter() throws Exception {
		createEntry("Leite vencido", 1, "2026-08-21").andExpect(status().isCreated());
		createEntry("Leite atenção", 1, "2026-08-29").andExpect(status().isCreated());
		createEntry("Leite observar", 1, "2026-08-30").andExpect(status().isCreated());
		createEntry("Leite seguro", 1, "2026-09-22").andExpect(status().isCreated());
		createEntry("Arroz vencido", 1, "2026-08-20").andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("query", "leite")
				.param("status", "EXPIRED")
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].productName").value("Leite vencido"));

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("query", "leite")
				.param("status", "ATTENTION")
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].productName").value("Leite atenção"));

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("query", "leite")
				.param("status", "WATCH")
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].productName").value("Leite observar"));

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("query", "leite")
				.param("status", "OK")
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].productName").value("Leite seguro"));
	}

	@Test
	void continuesFilteredProductSearchWithoutDuplicatesOrUnrelatedEntries() throws Exception {
		long firstEntryId = createdEntryId("Leite A", 1, "2030-01-01");
		long secondEntryId = createdEntryId("Leite B", 1, "2030-01-02");
		createdEntryId("Arroz", 1, "2030-01-03");

		MvcResult firstSlice = mockMvc.perform(get("/api/v1/stock-entries")
				.param("size", "1")
				.param("query", "leite")
				.param("status", "OK")
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].id").value(firstEntryId))
			.andExpect(jsonPath("$.hasNext").value(true))
			.andReturn();
		String cursorExpirationDate = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorExpirationDate");
		String cursorCreatedAt = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorCreatedAt");
		Number cursorId = com.jayway.jsonpath.JsonPath.read(
				firstSlice.getResponse().getContentAsString(), "$.nextCursorId");

		mockMvc.perform(get("/api/v1/stock-entries")
				.param("size", "1")
				.param("query", "leite")
				.param("status", "OK")
				.param("cursorExpirationDate", cursorExpirationDate)
				.param("cursorCreatedAt", cursorCreatedAt)
				.param("cursorId", cursorId.toString())
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].id").value(secondEntryId))
			.andExpect(jsonPath("$.hasNext").value(false));
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

	private int entryCreationStatus(String productName, String barcode, String expirationDate) {
		return entryCreationStatus(productName, barcode, null, expirationDate);
	}

	private int entryCreationStatus(String productName, String barcode, String category, String expirationDate) {
		try {
			String barcodeProperty = barcode == null ? "" : ",\"barcode\":\"%s\"".formatted(barcode);
			String categoryProperty = category == null ? "" : ",\"category\":\"%s\"".formatted(category);
			return mockMvc.perform(post("/api/v1/stock-entries")
					.with(operator())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"productName":"%s","quantity":1,"expirationDate":"%s"%s%s}
							""".formatted(productName, expirationDate, barcodeProperty, categoryProperty)))
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
