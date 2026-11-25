package com.kuador.platodo.api.bookings.infrastructure.web;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.CreatePendingBooking;

/** {@code CreatePendingBookingRequest} schema of the API contract. A missing field is a malformed request. */
record CreatePendingBookingRequest(UUID courtId, Instant start, Boolean acknowledgeOwnOverlap) {

    CreatePendingBookingRequest {
        Objects.requireNonNull(courtId, "courtId");
        Objects.requireNonNull(start, "start");
    }

    CreatePendingBooking.Command toCommand() {
        return new CreatePendingBooking.Command(courtId, start, Boolean.TRUE.equals(acknowledgeOwnOverlap));
    }
}
