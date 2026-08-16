package io.github.vitaa1.vencefacil.inventory;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "stock_entry_id", nullable = false)
	private StockEntry stockEntry;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 20)
	private MovementType movementType;

	@Column(nullable = false)
	private int quantity;

	@Column(length = 30)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected StockMovement() {
	}

	StockMovement(StockEntry stockEntry, MovementType movementType, int quantity, Instant createdAt) {
		this.stockEntry = stockEntry;
		this.movementType = movementType;
		this.quantity = quantity;
		this.createdAt = createdAt;
	}

	StockMovement(StockEntry stockEntry, int quantity, WithdrawalReason reason, Instant createdAt) {
		this.stockEntry = stockEntry;
		this.movementType = MovementType.WITHDRAWAL;
		this.quantity = quantity;
		this.reason = reason.name();
		this.createdAt = createdAt;
	}

	MovementType getMovementType() {
		return movementType;
	}

	int getQuantity() {
		return quantity;
	}

	String getReason() {
		return reason;
	}

}
