package com.kuador.platodo.api.bookings.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Half-open time range {@code [start, end)}: 20:00-21:00 and 21:00-22:00 are adjacent, not overlapping.
 */
public record TimePeriod(Instant start, Instant end) {

    public TimePeriod {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Period start must be before its end: " + start + " - " + end);
        }
    }
}
