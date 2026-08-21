package io.github.vitaa1.gestorvp.inventory;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_entries")
class StockEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "initial_quantity", nullable = false)
	private int initialQuantity;

	@Column(name = "available_quantity", nullable = false)
	private int availableQuantity;

	@Column(name = "expiration_date", nullable = false)
	private LocalDate expirationDate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected StockEntry() {
	}

	StockEntry(Product product, int quantity, LocalDate expirationDate, Instant createdAt) {
		this.product = product;
		this.initialQuantity = quantity;
		this.availableQuantity = quantity;
		this.expirationDate = expirationDate;
		this.createdAt = createdAt;
	}

	Long getId() {
		return id;
	}

	Product getProduct() {
		return product;
	}

	int getAvailableQuantity() {
		return availableQuantity;
	}

	int getInitialQuantity() {
		return initialQuantity;
	}

	LocalDate getExpirationDate() {
		return expirationDate;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	void validateWithdrawal(int quantity, WithdrawalReason reason, LocalDate today) {
		if (quantity <= 0) {
			throw new InvalidWithdrawalException("A quantidade deve ser maior que zero.");
		}
		if (expirationDate.isBefore(today) && !reason.isAllowedForExpiredEntry()) {
			throw new InvalidWithdrawalException(
					"Entradas vencidas aceitam somente os motivos Perdi ou Venceu.");
		}
	}

}
