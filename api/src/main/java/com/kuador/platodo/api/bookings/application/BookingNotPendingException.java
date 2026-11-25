package com.kuador.platodo.api.bookings.application;

import java.util.UUID;

import com.kuador.platodo.api.bookings.domain.BookingStatus;

/** The operation needs a pending booking, but the booking is in another effective status. */
public class BookingNotPendingException extends RuntimeException {

    public BookingNotPendingException(UUID bookingId, BookingStatus effectiveStatus) {
        super("Booking " + bookingId + " is " + effectiveStatus + ", not PENDING");
    }
}
