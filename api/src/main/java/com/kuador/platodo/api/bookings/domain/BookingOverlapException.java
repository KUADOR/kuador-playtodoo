package com.kuador.platodo.api.bookings.domain;

import java.util.UUID;

/**
 * The requested period overlaps an active booking (pending or confirmed) or block on the same court.
 * The database exclusion constraint is the source of this rule (ADR-006).
 */
public class BookingOverlapException extends RuntimeException {

    private final UUID courtId;
    private final TimePeriod period;

    public BookingOverlapException(UUID courtId, TimePeriod period, Throwable cause) {
        super("Court " + courtId + " already has an active booking overlapping " + period, cause);
        this.courtId = courtId;
        this.period = period;
    }

    public UUID courtId() {
        return courtId;
    }

    public TimePeriod period() {
        return period;
    }
}
