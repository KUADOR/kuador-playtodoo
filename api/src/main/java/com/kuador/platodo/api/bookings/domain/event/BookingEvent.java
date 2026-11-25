package com.kuador.platodo.api.bookings.domain.event;

import java.util.UUID;

/** Event published by the Bookings module about one booking. Events never carry personal data. */
public interface BookingEvent {

    UUID bookingId();
}
