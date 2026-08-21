package io.github.vitaa1.gestorvp.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WithdrawStockRequest(
		@Positive int quantity,
		@NotNull WithdrawalReason reason) {
}
