package io.github.vitaa1.gestorvp.inventory;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateStockEntryRequest(
		@NotBlank(message = "Informe o nome do produto")
		@Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
		String productName,
		@NotNull(message = "Informe a quantidade")
		@Positive(message = "A quantidade deve ser maior que zero")
		Integer quantity,
		@NotNull(message = "Informe a data de validade")
		LocalDate expirationDate) {
}
