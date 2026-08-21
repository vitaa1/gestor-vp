package io.github.vitaa1.vencefacil.inventory;

public enum WithdrawalReason {
	SOLD("Vendi"),
	USED("Usei"),
	DONATED("Doei"),
	LOST("Perdi"),
	EXPIRED("Venceu");

	private final String label;

	WithdrawalReason(String label) {
		this.label = label;
	}

	String getLabel() {
		return label;
	}

	boolean isAllowedForExpiredEntry() {
		return this == LOST || this == EXPIRED;
	}
}
