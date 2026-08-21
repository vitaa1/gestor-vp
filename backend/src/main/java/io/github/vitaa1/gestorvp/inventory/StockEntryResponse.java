package io.github.vitaa1.gestorvp.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record StockEntryResponse(
		Long id,
		String productName,
		int quantity,
		LocalDate expirationDate,
		ExpirationStatus status,
		String statusLabel,
		long daysUntilExpiration,
		Instant createdAt) {

	static StockEntryResponse from(StockEntry entry, LocalDate today) {
		ExpirationStatus status = ExpirationStatus.from(entry.getExpirationDate(), today);
		return new StockEntryResponse(
				entry.getId(),
				entry.getProduct().getName(),
				entry.getAvailableQuantity(),
				entry.getExpirationDate(),
				status,
				status.getLabel(),
				ChronoUnit.DAYS.between(today, entry.getExpirationDate()),
				entry.getCreatedAt());
	}
}
