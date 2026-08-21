package io.github.vitaa1.vencefacil.inventory;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

	@EntityGraph(attributePaths = "stockEntry.product")
	@Query("""
			select movement from StockMovement movement
			order by movement.createdAt desc, movement.id desc
			""")
	List<StockMovement> findFirstSlice(Pageable pageable);

	@EntityGraph(attributePaths = "stockEntry.product")
	@Query("""
			select movement from StockMovement movement
			where movement.createdAt < :cursorCreatedAt
			   or (movement.createdAt = :cursorCreatedAt and movement.id < :cursorId)
			order by movement.createdAt desc, movement.id desc
			""")
	List<StockMovement> findSliceBefore(
			@Param("cursorCreatedAt") Instant cursorCreatedAt,
			@Param("cursorId") long cursorId,
			Pageable pageable);
}
