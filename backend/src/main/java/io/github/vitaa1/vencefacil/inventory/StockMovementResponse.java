package io.github.vitaa1.vencefacil.inventory;

import java.time.Instant;
import java.time.LocalDate;

public record StockMovementResponse(
		Long id,
		Long stockEntryId,
		String productName,
		LocalDate expirationDate,
		MovementType type,
		String typeLabel,
		int quantity,
		WithdrawalReason reason,
		String reasonLabel,
		Instant createdAt,
		boolean entryClosed) {

	static StockMovementResponse from(StockMovement movement) {
		StockEntry entry = movement.getStockEntry();
		WithdrawalReason reason = movement.getReason() == null
				? null
				: WithdrawalReason.valueOf(movement.getReason());
		return new StockMovementResponse(
				movement.getId(),
				entry.getId(),
				entry.getProduct().getName(),
				entry.getExpirationDate(),
				movement.getMovementType(),
				movement.getMovementType().getLabel(),
				movement.getQuantity(),
				reason,
				reason == null ? null : reason.getLabel(),
				movement.getCreatedAt(),
				entry.getAvailableQuantity() == 0);
	}
}
