package io.github.vitaa1.vencefacil.inventory;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1/stock-movements")
class StockMovementController {

	private final StockMovementService stockMovementService;

	StockMovementController(StockMovementService stockMovementService) {
		this.stockMovementService = stockMovementService;
	}

	@GetMapping
	StockMovementPageResponse list(
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) Instant cursorCreatedAt,
			@RequestParam(required = false) @Min(1) Long cursorId) {
		if ((cursorCreatedAt == null) != (cursorId == null)) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Os dois parâmetros do cursor devem ser informados juntos.");
		}
		return stockMovementService.list(size, cursorCreatedAt, cursorId);
	}
}
