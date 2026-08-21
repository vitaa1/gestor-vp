package io.github.vitaa1.gestorvp.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record StockEntryResponse(
		Long id,
		String productName,
		String barcode,
		String category,
		int quantity,
		LocalDate expirationDate,
		BigDecimal unitCost,
		String supplier,
		String batchNumber,
		ExpirationStatus status,
		String statusLabel,
		long daysUntilExpiration,
		Instant createdAt) {

	static StockEntryResponse from(StockEntry entry, LocalDate today) {
		ExpirationStatus status = ExpirationStatus.from(entry.getExpirationDate(), today);
		return new StockEntryResponse(
				entry.getId(),
				entry.getProduct().getName(),
				entry.getProduct().getBarcode(),
				entry.getProduct().getCategory(),
				entry.getAvailableQuantity(),
				entry.getExpirationDate(),
				entry.getUnitCost(),
				entry.getSupplier(),
				entry.getBatchNumber(),
				status,
				status.getLabel(),
				ChronoUnit.DAYS.between(today, entry.getExpirationDate()),
				entry.getCreatedAt());
	}
}
