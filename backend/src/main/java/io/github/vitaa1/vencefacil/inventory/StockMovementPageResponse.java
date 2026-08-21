package io.github.vitaa1.vencefacil.inventory;

import java.util.List;

public record StockMovementPageResponse(
		List<StockMovementResponse> content,
		int size,
		boolean hasNext,
		java.time.Instant nextCursorCreatedAt,
		Long nextCursorId) {

	static StockMovementPageResponse from(
			List<StockMovementResponse> content,
			int size,
			boolean hasNext) {
		StockMovementResponse cursorMovement = hasNext ? content.getLast() : null;
		return new StockMovementPageResponse(
				content,
				size,
				hasNext,
				cursorMovement == null ? null : cursorMovement.createdAt(),
				cursorMovement == null ? null : cursorMovement.id());
	}
}
