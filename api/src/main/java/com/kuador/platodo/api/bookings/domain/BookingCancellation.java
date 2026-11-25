package com.kuador.platodo.api.bookings.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** When and by whom a booking was cancelled. */
public record BookingCancellation(Instant cancelledAt, UUID cancelledBy) {

    public BookingCancellation {
        Objects.requireNonNull(cancelledAt, "cancelledAt");
        Objects.requireNonNull(cancelledBy, "cancelledBy");
    }
}
