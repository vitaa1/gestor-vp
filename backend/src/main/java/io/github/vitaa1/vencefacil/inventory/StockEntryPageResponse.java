package io.github.vitaa1.vencefacil.inventory;

import java.util.List;

import org.springframework.data.domain.Page;

public record StockEntryPageResponse(
		List<StockEntryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static StockEntryPageResponse from(Page<StockEntryResponse> result) {
		return new StockEntryPageResponse(
				result.getContent(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}
}
