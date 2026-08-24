package io.github.vitaa1.gestorvp.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockEntryRepository extends JpaRepository<StockEntry, Long> {

	@EntityGraph(attributePaths = "product")
	@Query("""
			select entry from StockEntry entry
			where entry.availableQuantity > 0
			  and entry.product.searchName like concat('%', :normalizedQuery, '%') escape '!'
			  and entry.expirationDate >= :minimumExpirationDate
			  and entry.expirationDate <= :maximumExpirationDate
			order by entry.expirationDate, entry.createdAt, entry.id
			""")
	List<StockEntry> findFirstActiveSlice(
			@Param("normalizedQuery") String normalizedQuery,
			@Param("minimumExpirationDate") LocalDate minimumExpirationDate,
			@Param("maximumExpirationDate") LocalDate maximumExpirationDate,
			Pageable pageable);

	@EntityGraph(attributePaths = "product")
	@Query("""
			select entry from StockEntry entry
			where entry.availableQuantity > 0
			  and entry.product.searchName like concat('%', :normalizedQuery, '%') escape '!'
			  and entry.expirationDate >= :minimumExpirationDate
			  and entry.expirationDate <= :maximumExpirationDate
			  and (entry.expirationDate > :cursorExpirationDate
			       or (entry.expirationDate = :cursorExpirationDate and entry.createdAt > :cursorCreatedAt)
			       or (entry.expirationDate = :cursorExpirationDate
			           and entry.createdAt = :cursorCreatedAt and entry.id > :cursorId))
			order by entry.expirationDate, entry.createdAt, entry.id
			""")
	List<StockEntry> findActiveSliceAfter(
			@Param("normalizedQuery") String normalizedQuery,
			@Param("minimumExpirationDate") LocalDate minimumExpirationDate,
			@Param("maximumExpirationDate") LocalDate maximumExpirationDate,
			@Param("cursorExpirationDate") LocalDate cursorExpirationDate,
			@Param("cursorCreatedAt") Instant cursorCreatedAt,
			@Param("cursorId") long cursorId,
			Pageable pageable);

	@EntityGraph(attributePaths = "product")
	@Query("select entry from StockEntry entry where entry.id = :entryId")
	Optional<StockEntry> findDetailsById(@Param("entryId") long entryId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update StockEntry entry
			set entry.availableQuantity = entry.availableQuantity - :quantity
			where entry.id = :entryId and entry.availableQuantity >= :quantity
			""")
	int withdrawIfAvailable(@Param("entryId") long entryId, @Param("quantity") int quantity);

}
