package com.kuador.platodo.api.bookings.application;

import java.time.Instant;

/** The queried occupancy range is empty, inverted or longer than allowed. */
public class InvalidOccupancyRangeException extends RuntimeException {

    public InvalidOccupancyRangeException(Instant from, Instant to) {
        super("`to` must be after `from` and at most " + GetCourtOccupancy.MAX_RANGE.toDays()
                + " days later: " + from + " - " + to);
    }
}
