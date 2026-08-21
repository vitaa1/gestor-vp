package io.github.vitaa1.gestorvp.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
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
		LocalDate expirationDate,
		@Pattern(regexp = "^$|[0-9]{8,14}", message = "O código de barras deve ter de 8 a 14 dígitos")
		String barcode,
		@Size(max = 120, message = "A categoria deve ter no máximo 120 caracteres")
		String category,
		@DecimalMin(value = "0.00", message = "O preço de custo não pode ser negativo")
		@Digits(integer = 10, fraction = 2, message = "O preço de custo deve ter no máximo duas casas decimais")
		BigDecimal unitCost,
		@Size(max = 120, message = "O fornecedor deve ter no máximo 120 caracteres")
		String supplier,
		@Size(max = 120, message = "O número do lote deve ter no máximo 120 caracteres")
		String batchNumber) {
}
