package io.github.vitaa1.vencefacil.inventory;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/stock-entries")
class StockEntryController {

	private final StockEntryService stockEntryService;

	StockEntryController(StockEntryService stockEntryService) {
		this.stockEntryService = stockEntryService;
	}

	@PostMapping
	ResponseEntity<StockEntryResponse> create(@Valid @RequestBody CreateStockEntryRequest request) {
		StockEntryResponse response = stockEntryService.create(request);
		return ResponseEntity.created(URI.create("/api/stock-entries/" + response.id())).body(response);
	}

	@GetMapping
	StockEntryPageResponse listActive(
			@RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int page,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
		return stockEntryService.listActive(page, size);
	}
}
