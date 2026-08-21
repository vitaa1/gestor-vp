package io.github.vitaa1.vencefacil.inventory;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/api/v1/stock-entries")
class StockEntryController {

	private final StockEntryService stockEntryService;
	private final BusinessDateProvider businessDateProvider;

	StockEntryController(StockEntryService stockEntryService, BusinessDateProvider businessDateProvider) {
		this.stockEntryService = stockEntryService;
		this.businessDateProvider = businessDateProvider;
	}

	@PostMapping
	ResponseEntity<StockEntryResponse> create(
			@RequestHeader(name = BusinessDateProvider.USER_TIME_ZONE_HEADER, required = false) @Size(max = 100) String userTimeZone,
			@Valid @RequestBody CreateStockEntryRequest request) {
		StockEntryResponse response = stockEntryService.create(request,
				businessDateProvider.currentDate(userTimeZone));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	StockEntryPageResponse listActive(
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 120) String query,
			@RequestParam(required = false) ExpirationStatus status,
			@RequestParam(required = false) LocalDate cursorExpirationDate,
			@RequestParam(required = false) Instant cursorCreatedAt,
			@RequestParam(required = false) @Min(1) Long cursorId,
			@RequestHeader(name = BusinessDateProvider.USER_TIME_ZONE_HEADER, required = false) @Size(max = 100) String userTimeZone) {
		boolean hasAnyCursorValue = cursorExpirationDate != null || cursorCreatedAt != null || cursorId != null;
		boolean hasCompleteCursor = cursorExpirationDate != null && cursorCreatedAt != null && cursorId != null;
		if (hasAnyCursorValue && !hasCompleteCursor) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Os três parâmetros do cursor devem ser informados juntos.");
		}
		return stockEntryService.listActive(
				size,
				query,
				status,
				cursorExpirationDate,
				cursorCreatedAt,
				cursorId,
				businessDateProvider.currentDate(userTimeZone));
	}

	@GetMapping("/{entryId}")
	StockEntryDetailsResponse details(@PathVariable @Min(1) long entryId,
			@RequestHeader(name = BusinessDateProvider.USER_TIME_ZONE_HEADER, required = false) @Size(max = 100) String userTimeZone) {
		return stockEntryService.details(entryId, businessDateProvider.currentDate(userTimeZone));
	}

	@PostMapping("/{entryId}/withdrawals")
	StockEntryDetailsResponse withdraw(@PathVariable @Min(1) long entryId,
			@RequestHeader(name = BusinessDateProvider.USER_TIME_ZONE_HEADER, required = false) @Size(max = 100) String userTimeZone,
			@Valid @RequestBody WithdrawStockRequest request) {
		BusinessDateProvider.BusinessDates dates = businessDateProvider.datesFor(userTimeZone);
		return stockEntryService.withdraw(entryId, request, dates.establishmentDate(), dates.operatorDate());
	}
}
