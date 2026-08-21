package io.github.vitaa1.gestorvp.inventory;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class StockMovementService {

	private final StockMovementRepository stockMovementRepository;

	StockMovementService(StockMovementRepository stockMovementRepository) {
		this.stockMovementRepository = stockMovementRepository;
	}

	@Transactional(readOnly = true)
	StockMovementPageResponse list(int size, Instant cursorCreatedAt, Long cursorId) {
		PageRequest pageRequest = PageRequest.of(0, size + 1);
		List<StockMovement> movements = cursorCreatedAt == null
				? stockMovementRepository.findFirstSlice(pageRequest)
				: stockMovementRepository.findSliceBefore(cursorCreatedAt, cursorId, pageRequest);
		boolean hasNext = movements.size() > size;
		List<StockMovementResponse> content = movements.stream()
			.limit(size)
			.map(StockMovementResponse::from)
			.toList();
		return StockMovementPageResponse.from(content, size, hasNext);
	}
}
