package io.github.vitaa1.vencefacil.inventory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

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
		String productName = normalizeWhitespace(request.productName());
		String normalizedName = productName.toLowerCase(Locale.ROOT);
		Instant now = clock.instant();

		productRepository.insertIfAbsent(productName, normalizedName, now);
		Product product = productRepository.findByNormalizedName(normalizedName)
			.orElseThrow(() -> new IllegalStateException("Product was not available after creation"));
		StockEntry entry = stockEntryRepository.save(
				new StockEntry(product, request.quantity(), request.expirationDate(), now));
		stockMovementRepository.save(new StockMovement(entry, MovementType.ENTRY, request.quantity(), now));

		return StockEntryResponse.from(entry, today);
	}

	@Transactional(readOnly = true)
	StockEntryPageResponse listActive(int size, LocalDate cursorExpirationDate, Instant cursorCreatedAt,
			Long cursorId, LocalDate today) {
		PageRequest pageRequest = PageRequest.of(0, size + 1);
		List<StockEntry> entries = cursorExpirationDate == null
				? stockEntryRepository.findFirstActiveSlice(pageRequest)
				: stockEntryRepository.findActiveSliceAfter(
						cursorExpirationDate, cursorCreatedAt, cursorId, pageRequest);
		boolean hasNext = entries.size() > size;
		List<StockEntryResponse> content = entries.stream()
			.limit(size)
			.map(entry -> StockEntryResponse.from(entry, today))
			.toList();
		return StockEntryPageResponse.from(content, size, hasNext);
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

	private String normalizeWhitespace(String value) {
		return value.trim().replaceAll("\\s+", " ");
	}
}
