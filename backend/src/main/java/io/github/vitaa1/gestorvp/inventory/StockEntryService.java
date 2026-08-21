package io.github.vitaa1.gestorvp.inventory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class StockEntryService {

	private final ProductRepository productRepository;
	private final StockEntryRepository stockEntryRepository;
	private final StockMovementRepository stockMovementRepository;
	private final Clock clock;

	StockEntryService(ProductRepository productRepository, StockEntryRepository stockEntryRepository,
			StockMovementRepository stockMovementRepository, Clock clock) {
		this.productRepository = productRepository;
		this.stockEntryRepository = stockEntryRepository;
		this.stockMovementRepository = stockMovementRepository;
		this.clock = clock;
	}

	@Transactional
	StockEntryResponse create(CreateStockEntryRequest request, LocalDate today) {
		String productName = ProductNameNormalizer.displayName(request.productName());
		String normalizedName = ProductNameNormalizer.legacyNormalizedName(productName);
		String searchName = ProductNameNormalizer.searchName(productName);
		Instant now = clock.instant();

		Product product = productRepository.findFirstBySearchNameOrderById(searchName).orElseGet(() -> {
			productRepository.insertIfAbsent(productName, normalizedName, now);
			return productRepository.findFirstBySearchNameOrderById(searchName)
					.orElseThrow(() -> new IllegalStateException("Product was not available after creation"));
		});
		StockEntry entry = stockEntryRepository.save(
				new StockEntry(product, request.quantity(), request.expirationDate(), now));
		stockMovementRepository.save(new StockMovement(entry, MovementType.ENTRY, request.quantity(), now));

		return StockEntryResponse.from(entry, today);
	}

	@Transactional(readOnly = true)
	StockEntryPageResponse listActive(int size, String query, ExpirationStatus status,
			LocalDate cursorExpirationDate, Instant cursorCreatedAt, Long cursorId, LocalDate today) {
		PageRequest pageRequest = PageRequest.of(0, size + 1);
		ExpirationDateRange range = ExpirationDateRange.from(status, today);
		String normalizedQuery = query == null || query.isBlank()
				? ""
				: ProductNameNormalizer.searchName(query);
		List<StockEntry> entries = cursorExpirationDate == null
				? stockEntryRepository.findFirstActiveSlice(
						normalizedQuery, range.minimum(), range.maximum(), pageRequest)
				: stockEntryRepository.findActiveSliceAfter(
						normalizedQuery, range.minimum(), range.maximum(),
						cursorExpirationDate, cursorCreatedAt, cursorId, pageRequest);
		boolean hasNext = entries.size() > size;
		List<StockEntryResponse> content = entries.stream()
			.limit(size)
			.map(entry -> StockEntryResponse.from(entry, today))
			.toList();
		return StockEntryPageResponse.from(content, size, hasNext);
	}

	private record ExpirationDateRange(LocalDate minimum, LocalDate maximum) {
		private static final LocalDate EARLIEST_SUPPORTED_DATE = LocalDate.of(1, 1, 1);
		private static final LocalDate LATEST_SUPPORTED_DATE = LocalDate.of(9999, 12, 31);

		static ExpirationDateRange from(ExpirationStatus status, LocalDate today) {
			if (status == null) {
				return new ExpirationDateRange(EARLIEST_SUPPORTED_DATE, LATEST_SUPPORTED_DATE);
			}
			return switch (status) {
				case EXPIRED -> new ExpirationDateRange(EARLIEST_SUPPORTED_DATE, today.minusDays(1));
				case ATTENTION -> new ExpirationDateRange(today, today.plusDays(7));
				case WATCH -> new ExpirationDateRange(today.plusDays(8), today.plusDays(30));
				case OK -> new ExpirationDateRange(today.plusDays(31), LATEST_SUPPORTED_DATE);
			};
		}
	}

	@Transactional(readOnly = true)
	StockEntryDetailsResponse details(long entryId, LocalDate today) {
		StockEntry entry = findEntry(entryId);
		return StockEntryDetailsResponse.from(entry, today);
	}

	@Transactional
	StockEntryDetailsResponse withdraw(long entryId, WithdrawStockRequest request, LocalDate establishmentDate,
			LocalDate operatorDate) {
		StockEntry entry = findEntry(entryId);
		entry.validateWithdrawal(request.quantity(), request.reason(), establishmentDate);

		if (stockEntryRepository.withdrawIfAvailable(entryId, request.quantity()) == 0) {
			throw new InvalidWithdrawalException("A quantidade informada supera o saldo disponível.");
		}

		StockEntry updatedEntry = findEntry(entryId);
		stockMovementRepository.save(
				new StockMovement(updatedEntry, request.quantity(), request.reason(), clock.instant()));
		return StockEntryDetailsResponse.from(updatedEntry, operatorDate);
	}

	private StockEntry findEntry(long entryId) {
		return stockEntryRepository.findDetailsById(entryId)
			.orElseThrow(() -> new StockEntryNotFoundException(entryId));
	}

}
