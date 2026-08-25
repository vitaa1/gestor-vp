package io.github.vitaa1.gestorvp.inventory;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import io.github.vitaa1.gestorvp.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
		"app.security.username=test-operator",
		"app.security.password=test-password"
})
class StockMovementControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockMovementRepository stockMovementRepository;

	@Autowired
	private StockEntryRepository stockEntryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	void cleanDatabase() {
		stockMovementRepository.deleteAll();
		stockEntryRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void listsOneMovementPerRowWithRecentRecordsFirstAndPagination() throws Exception {
		long milkEntryId = createdEntryId("Leite Integral", 12, "2030-01-10");
		withdraw(milkEntryId, 5, "SOLD");
		long riceEntryId = createdEntryId("Arroz Integral", 4, "2030-10-20");

		MvcResult firstPage = mockMvc.perform(get("/api/v1/stock-movements?size=2").with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].stockEntryId").value(riceEntryId))
			.andExpect(jsonPath("$.content[0].productName").value("Arroz Integral"))
			.andExpect(jsonPath("$.content[0].expirationDate").value("2030-10-20"))
			.andExpect(jsonPath("$.content[0].type").value("ENTRY"))
			.andExpect(jsonPath("$.content[0].typeLabel").value("Entrada"))
			.andExpect(jsonPath("$.content[0].quantity").value(4))
			.andExpect(jsonPath("$.content[0].createdAt").isString())
			.andExpect(jsonPath("$.content[1].stockEntryId").value(milkEntryId))
			.andExpect(jsonPath("$.content[1].type").value("WITHDRAWAL"))
			.andExpect(jsonPath("$.content[1].typeLabel").value("Retirada"))
			.andExpect(jsonPath("$.content[1].quantity").value(5))
			.andExpect(jsonPath("$.content[1].reason").value("SOLD"))
			.andExpect(jsonPath("$.content[1].reasonLabel").value("Venda"))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.hasNext").value(true))
			.andExpect(jsonPath("$.nextCursorCreatedAt").isString())
			.andExpect(jsonPath("$.nextCursorId").isNumber())
			.andReturn();
		String cursorCreatedAt = com.jayway.jsonpath.JsonPath.read(
				firstPage.getResponse().getContentAsString(), "$.nextCursorCreatedAt");
		Number cursorId = com.jayway.jsonpath.JsonPath.read(
				firstPage.getResponse().getContentAsString(), "$.nextCursorId");

		createdEntryId("Feijão Carioca", 6, "2031-02-15");

		mockMvc.perform(get("/api/v1/stock-movements?size=2&cursorCreatedAt={cursorCreatedAt}&cursorId={cursorId}",
				cursorCreatedAt, cursorId)
				.with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].stockEntryId").value(milkEntryId))
			.andExpect(jsonPath("$.content[0].type").value("ENTRY"))
			.andExpect(jsonPath("$.hasNext").value(false))
			.andExpect(jsonPath("$.nextCursorCreatedAt").doesNotExist())
			.andExpect(jsonPath("$.nextCursorId").doesNotExist());
	}

	@Test
	void keepsCursorPaginationStableWhenAnOlderIdentityCommitsBetweenRequests() throws Exception {
		long entryId = createdEntryId("Aveia", 10, "2030-08-01");
		stockMovementRepository.deleteAll();
		insertMovement(entryId, "2030-01-04T12:00:00Z");

		try (Connection pendingConnection = dataSource.getConnection()) {
			pendingConnection.setAutoCommit(false);
			insertMovement(pendingConnection, entryId, "2030-01-03T12:00:00Z");
			insertMovement(entryId, "2030-01-02T12:00:00Z");
			insertMovement(entryId, "2030-01-01T12:00:00Z");

			MvcResult firstPage = mockMvc.perform(get("/api/v1/stock-movements?size=2").with(operator()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].createdAt").value("2030-01-04T12:00:00Z"))
				.andExpect(jsonPath("$.content[1].createdAt").value("2030-01-02T12:00:00Z"))
				.andExpect(jsonPath("$.hasNext").value(true))
				.andReturn();
			String cursorCreatedAt = com.jayway.jsonpath.JsonPath.read(
					firstPage.getResponse().getContentAsString(), "$.nextCursorCreatedAt");
			Number cursorId = com.jayway.jsonpath.JsonPath.read(
					firstPage.getResponse().getContentAsString(), "$.nextCursorId");

			pendingConnection.commit();

			mockMvc.perform(get("/api/v1/stock-movements?size=2&cursorCreatedAt={cursorCreatedAt}&cursorId={cursorId}",
					cursorCreatedAt, cursorId).with(operator()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].createdAt").value("2030-01-01T12:00:00Z"))
				.andExpect(jsonPath("$.hasNext").value(false));
		}
	}

	@Test
	void keepsAClosedEntryAvailableForReadOnlyConsultationFromHistory() throws Exception {
		long entryId = createdEntryId("Pão de Forma", 8, "2030-01-01");
		withdraw(entryId, 8, "USED");

		mockMvc.perform(get("/api/v1/stock-movements").with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].stockEntryId").value(entryId))
			.andExpect(jsonPath("$.content[0].entryClosed").value(true));

		mockMvc.perform(get("/api/v1/stock-entries/{entryId}", entryId).with(operator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableQuantity").value(0));
	}

	@Test
	void protectsAndLimitsTheHistoryQuery() throws Exception {
		mockMvc.perform(get("/api/v1/stock-movements"))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/stock-movements?size=101").with(operator()))
			.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/stock-movements?cursorId=10").with(operator()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsAnInvalidWithdrawalReasonAtTheDatabaseBoundary() throws Exception {
		long entryId = createdEntryId("Café Torrado", 2, "2030-06-10");

		assertThatThrownBy(() -> jdbcTemplate.update("""
				insert into stock_movements
				    (stock_entry_id, movement_type, quantity, reason, created_at)
				values (?, 'WITHDRAWAL', 1, 'UNKNOWN', current_timestamp)
				""", entryId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private long createdEntryId(String productName, int quantity, String expirationDate) throws Exception {
		String response = mockMvc.perform(post("/api/v1/stock-entries")
				.with(operator())
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

	private void withdraw(long entryId, int quantity, String reason) throws Exception {
		mockMvc.perform(post("/api/v1/stock-entries/{entryId}/withdrawals", entryId)
				.with(operator())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":%d,"reason":"%s"}
						""".formatted(quantity, reason)))
			.andExpect(status().isOk());
	}

	private void insertMovement(long entryId, String createdAt) {
		jdbcTemplate.update("""
				insert into stock_movements
				    (stock_entry_id, movement_type, quantity, reason, created_at)
				values (?, 'ENTRY', 1, null, ?)
				""", entryId, Timestamp.from(Instant.parse(createdAt)));
	}

	private void insertMovement(Connection connection, long entryId, String createdAt) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("""
				insert into stock_movements
				    (stock_entry_id, movement_type, quantity, reason, created_at)
				values (?, 'ENTRY', 1, null, ?)
				""")) {
			statement.setLong(1, entryId);
			statement.setTimestamp(2, Timestamp.from(Instant.parse(createdAt)));
			statement.executeUpdate();
		}
	}

	private RequestPostProcessor operator() {
		return httpBasic("test-operator", "test-password");
	}
}
