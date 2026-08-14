package io.github.vitaa1.vencefacil.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
