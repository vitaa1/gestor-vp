package io.github.vitaa1.vencefacil.inventory;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "normalized_name", nullable = false, length = 120, unique = true)
	private String normalizedName;

	@Column(name = "search_name", nullable = false, length = 120)
	private String searchName;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Product() {
	}

	Product(String name, String normalizedName, Instant createdAt) {
		this.name = name;
		this.normalizedName = normalizedName;
		this.createdAt = createdAt;
	}

	String getName() {
		return name;
	}

}
