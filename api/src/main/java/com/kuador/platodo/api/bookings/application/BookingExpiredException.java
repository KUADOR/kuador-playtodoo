package com.kuador.platodo.api.bookings.application;

import java.util.UUID;

/** The pending hold of the booking ended before it was confirmed; its slot is released. */
public class BookingExpiredException extends RuntimeException {

    public BookingExpiredException(UUID bookingId) {
        super("Booking " + bookingId + " expired before confirmation");
    }
}
