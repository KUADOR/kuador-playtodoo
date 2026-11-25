package com.kuador.platodo.api.bookings.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingCancellation;
import com.kuador.platodo.api.bookings.domain.BookingStatus;

/** {@code PlayerBooking} schema of the API contract. */
record PlayerBookingResponse(
        UUID id,
        UUID courtId,
        Instant start,
        Instant end,
        BookingStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt) {

    /** Expects a booking already in its effective status. */
    static PlayerBookingResponse of(Booking booking) {
        BookingCancellation cancellation = booking.cancellation();
        return new PlayerBookingResponse(booking.id(), booking.courtId(), booking.period().start(),
                booking.period().end(), booking.status(), booking.expiresAt(), booking.createdAt(),
                booking.confirmedAt(), cancellation == null ? null : cancellation.cancelledAt());
    }
}
