package com.kuador.platodo.api.bookings.application;

import java.util.UUID;

/** The booking does not exist in the active tenant or, for a player, belongs to another player. */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID bookingId) {
        super("Booking " + bookingId + " not found");
    }
}
