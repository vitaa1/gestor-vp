package io.github.vitaa1.vencefacil.inventory;

import java.util.List;

public record StockEntryPageResponse(
		List<StockEntryResponse> content,
		int size,
		boolean hasNext,
		java.time.LocalDate nextCursorExpirationDate,
		java.time.Instant nextCursorCreatedAt,
		Long nextCursorId) {

	static StockEntryPageResponse from(List<StockEntryResponse> content, int size, boolean hasNext) {
		StockEntryResponse cursorEntry = hasNext ? content.getLast() : null;
		return new StockEntryPageResponse(
				content,
				size,
				hasNext,
				cursorEntry == null ? null : cursorEntry.expirationDate(),
				cursorEntry == null ? null : cursorEntry.createdAt(),
				cursorEntry == null ? null : cursorEntry.id());
	}
}
