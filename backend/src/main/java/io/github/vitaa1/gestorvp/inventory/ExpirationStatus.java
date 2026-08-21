package io.github.vitaa1.gestorvp.inventory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum ExpirationStatus {
	EXPIRED("Vencido"),
	ATTENTION("Atenção"),
	WATCH("Fique de olho"),
	OK("Tudo certo");

	private final String label;

	ExpirationStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	static ExpirationStatus from(LocalDate expirationDate, LocalDate today) {
		long days = ChronoUnit.DAYS.between(today, expirationDate);

		if (days < 0) {
			return EXPIRED;
		}
		if (days <= 7) {
			return ATTENTION;
		}
		if (days <= 30) {
			return WATCH;
		}
		return OK;
	}
}
