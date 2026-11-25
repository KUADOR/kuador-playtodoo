package com.kuador.platodo.api.bookings.application.port.out;

import com.kuador.platodo.api.bookings.domain.event.BookingEvent;

/**
 * Publishes the domain events of the Bookings module. Called inside the transaction that changed the
 * booking, so after-commit listeners only see committed changes.
 */
public interface BookingEventPublisher {

    void publish(BookingEvent event);
}
