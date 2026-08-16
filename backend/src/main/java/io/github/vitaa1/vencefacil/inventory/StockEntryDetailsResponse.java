package io.github.vitaa1.vencefacil.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record StockEntryDetailsResponse(
		Long id,
		String productName,
		int initialQuantity,
		int availableQuantity,
		LocalDate expirationDate,
		ExpirationStatus status,
		String statusLabel,
		long daysUntilExpiration,
		Instant createdAt) {

	static StockEntryDetailsResponse from(StockEntry entry, LocalDate today) {
		ExpirationStatus status = ExpirationStatus.from(entry.getExpirationDate(), today);
		return new StockEntryDetailsResponse(
				entry.getId(),
				entry.getProduct().getName(),
				entry.getInitialQuantity(),
				entry.getAvailableQuantity(),
				entry.getExpirationDate(),
				status,
				status.getLabel(),
				ChronoUnit.DAYS.between(today, entry.getExpirationDate()),
				entry.getCreatedAt());
	}
}
