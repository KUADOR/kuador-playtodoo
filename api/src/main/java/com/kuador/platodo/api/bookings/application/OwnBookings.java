package com.kuador.platodo.api.bookings.application;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.domain.Booking;

/** Looks up a booking of the current player; bookings of other players are reported as not found. */
final class OwnBookings {

    private OwnBookings() {
    }

    static Booking require(BookingRepository bookings, UUID bookingId, UUID playerId) {
        return bookings.findById(bookingId)
                .filter(booking -> booking.isHeldBy(playerId))
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }
}
