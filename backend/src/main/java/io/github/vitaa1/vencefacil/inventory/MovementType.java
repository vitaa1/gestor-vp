package io.github.vitaa1.vencefacil.inventory;

public enum MovementType {
	ENTRY("Entrada"),
	WITHDRAWAL("Retirada");

	private final String label;

	MovementType(String label) {
		this.label = label;
	}

	String getLabel() {
		return label;
	}
}
