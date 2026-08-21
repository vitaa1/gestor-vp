package io.github.vitaa1.vencefacil.inventory;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class BusinessDateProvider {

	public static final String USER_TIME_ZONE_HEADER = "X-User-Time-Zone";

	private final Clock clock;
	private final ZoneId defaultTimeZone;

	BusinessDateProvider(Clock clock, String defaultTimeZone) {
		this.clock = clock;
		this.defaultTimeZone = parse(defaultTimeZone);
	}

	public LocalDate currentDate(String userTimeZone) {
		return datesFor(userTimeZone).operatorDate();
	}

	BusinessDates datesFor(String userTimeZone) {
		ZoneId timeZone = userTimeZone == null || userTimeZone.isBlank()
				? defaultTimeZone
				: parse(userTimeZone);
		Instant now = clock.instant();
		return new BusinessDates(
				LocalDate.ofInstant(now, timeZone),
				LocalDate.ofInstant(now, defaultTimeZone));
	}

	public LocalDate defaultDate() {
		return LocalDate.ofInstant(clock.instant(), defaultTimeZone);
	}

	private ZoneId parse(String timeZone) {
		try {
			return ZoneId.of(timeZone);
		}
		catch (DateTimeException exception) {
			throw new InvalidUserTimeZoneException(exception);
		}
	}

	record BusinessDates(LocalDate operatorDate, LocalDate establishmentDate) {
	}
}
