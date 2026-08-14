package io.github.vitaa1.vencefacil.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface StockEntryRepository extends JpaRepository<StockEntry, Long> {

	@EntityGraph(attributePaths = "product")
	Page<StockEntry> findByAvailableQuantityGreaterThan(int quantity, Pageable pageable);

}
