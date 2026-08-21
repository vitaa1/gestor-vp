package io.github.vitaa1.gestorvp.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class BusinessDateProviderTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T00:30:00Z"), ZoneOffset.UTC);

	@Test
	void calculatesTheDateFromTheUsersTimeZone() {
		BusinessDateProvider provider = new BusinessDateProvider(CLOCK, "America/Sao_Paulo");

		assertThat(provider.currentDate("America/Sao_Paulo")).isEqualTo(LocalDate.of(2026, 8, 21));
		assertThat(provider.currentDate("UTC")).isEqualTo(LocalDate.of(2026, 8, 22));
	}

	@Test
	void usesTheConfiguredDefaultWithoutDependingOnTheServersTimeZone() {
		BusinessDateProvider provider = new BusinessDateProvider(CLOCK, "UTC");

		assertThat(provider.currentDate(null)).isEqualTo(LocalDate.of(2026, 8, 22));
		assertThat(provider.currentDate(" ")).isEqualTo(LocalDate.of(2026, 8, 22));
		assertThat(provider.defaultDate()).isEqualTo(LocalDate.of(2026, 8, 22));
		assertThat(provider.datesFor("America/Sao_Paulo"))
			.extracting(
					BusinessDateProvider.BusinessDates::operatorDate,
					BusinessDateProvider.BusinessDates::establishmentDate)
			.containsExactly(LocalDate.of(2026, 8, 21), LocalDate.of(2026, 8, 22));
	}

	@Test
	void rejectsAnInvalidTimeZone() {
		BusinessDateProvider provider = new BusinessDateProvider(CLOCK, "America/Sao_Paulo");

		assertThatThrownBy(() -> provider.currentDate("Mars/Olympus"))
			.isInstanceOf(InvalidUserTimeZoneException.class)
			.hasMessage("O fuso horário informado é inválido.");
	}
}
