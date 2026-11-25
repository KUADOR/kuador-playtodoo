package com.kuador.platodo.api.bookings.application.port.out;

import com.kuador.platodo.api.bookings.domain.BookingExpired;

/** Announces booking lifecycle changes to other modules. */
public interface BookingEventPublisher {

    void publish(BookingExpired event);
}
