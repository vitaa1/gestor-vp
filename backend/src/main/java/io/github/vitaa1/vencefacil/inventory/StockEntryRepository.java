package io.github.vitaa1.vencefacil.inventory;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockEntryRepository extends JpaRepository<StockEntry, Long> {

	@EntityGraph(attributePaths = "product")
	Page<StockEntry> findByAvailableQuantityGreaterThan(int quantity, Pageable pageable);

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
