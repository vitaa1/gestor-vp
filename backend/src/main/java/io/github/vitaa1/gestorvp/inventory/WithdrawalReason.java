package io.github.vitaa1.gestorvp.inventory;

public enum WithdrawalReason {
	SOLD("Venda"),
	USED("Uso"),
	DONATED("Doação"),
	LOST("Perda"),
	EXPIRED("Vencimento");

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
