package com.kuador.platodo.api.bookings.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingKind;
import com.kuador.platodo.api.bookings.domain.BookingStatus;

/**
 * {@code StaffBooking} schema of the API contract. The holder name of a pending booking is not disclosed (D9):
 * personal data is not exposed for a 5-minute hold. The holder phone is never part of the response.
 *
 * @param inactive whether the complex is suspended or archived (ADR-019): the entry is kept but has no effect
 */
record StaffBookingResponse(
        UUID id,
        UUID courtId,
        BookingKind kind,
        BookingStatus status,
        Instant start,
        Instant end,
        String holderName,
        String blockReason,
        boolean inactive) {

    /** Expects a booking already in its effective status. */
    static StaffBookingResponse of(Booking booking, boolean inactive) {
        return new StaffBookingResponse(booking.id(), booking.courtId(), booking.kind(), booking.status(),
                booking.period().start(), booking.period().end(), disclosedHolderName(booking), booking.blockReason(),
                inactive);
    }

    private static String disclosedHolderName(Booking booking) {
        if (booking.holder() == null || booking.status() == BookingStatus.PENDING) {
            return null;
        }
        return booking.holder().name();
    }
}
