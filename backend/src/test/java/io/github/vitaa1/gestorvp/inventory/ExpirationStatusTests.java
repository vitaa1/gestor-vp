package io.github.vitaa1.gestorvp.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ExpirationStatusTests {

	private static final LocalDate TODAY = LocalDate.of(2026, 8, 14);

	@Test
	void classifiesExpirationBoundaries() {
		assertThat(ExpirationStatus.from(TODAY.minusDays(1), TODAY)).isEqualTo(ExpirationStatus.EXPIRED);
		assertThat(ExpirationStatus.from(TODAY, TODAY)).isEqualTo(ExpirationStatus.ATTENTION);
		assertThat(ExpirationStatus.from(TODAY.plusDays(7), TODAY)).isEqualTo(ExpirationStatus.ATTENTION);
		assertThat(ExpirationStatus.from(TODAY.plusDays(8), TODAY)).isEqualTo(ExpirationStatus.WATCH);
		assertThat(ExpirationStatus.from(TODAY.plusDays(30), TODAY)).isEqualTo(ExpirationStatus.WATCH);
		assertThat(ExpirationStatus.from(TODAY.plusDays(31), TODAY)).isEqualTo(ExpirationStatus.OK);
	}
}
