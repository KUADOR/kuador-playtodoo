package com.kuador.platodo.api.bookings.application;

import java.time.Duration;
import java.time.Instant;

import com.kuador.platodo.api.bookings.domain.TimePeriod;

/** Range of a calendar query ({@code from} inclusive, {@code to} exclusive), as limited by the API contract. */
public final class CalendarRange {

    /** Longest range a client may query at once. */
    public static final Duration MAX_RANGE = Duration.ofDays(31);

    private CalendarRange() {
    }

    /**
     * @throws InvalidOccupancyRangeException if {@code to} is not after {@code from} or the range is too long
     */
    static TimePeriod of(Instant from, Instant to) {
        if (!from.isBefore(to) || Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw new InvalidOccupancyRangeException(from, to);
        }
        return new TimePeriod(from, to);
    }
}
