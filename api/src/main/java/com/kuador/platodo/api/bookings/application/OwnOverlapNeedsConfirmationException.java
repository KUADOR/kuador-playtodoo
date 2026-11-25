package com.kuador.platodo.api.bookings.application;

import com.kuador.platodo.api.bookings.domain.Booking;

/**
 * The player already holds an active booking in this complex that overlaps the requested period.
 * The player may retry acknowledging the overlap (for example, to book a court for teammates).
 */
public class OwnOverlapNeedsConfirmationException extends RuntimeException {

    private final transient Booking existingBooking;

    public OwnOverlapNeedsConfirmationException(Booking existingBooking) {
        super("Player already holds booking " + existingBooking.id() + " overlapping the requested period");
        this.existingBooking = existingBooking;
    }

    /** The overlapping booking, with its effective status. */
    public Booking existingBooking() {
        return existingBooking;
    }
}
