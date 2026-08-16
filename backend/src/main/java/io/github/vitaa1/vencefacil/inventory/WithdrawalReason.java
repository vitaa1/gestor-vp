package io.github.vitaa1.vencefacil.inventory;

public enum WithdrawalReason {
	SOLD,
	USED,
	DONATED,
	LOST,
	EXPIRED;

	boolean isAllowedForExpiredEntry() {
		return this == LOST || this == EXPIRED;
	}
}
