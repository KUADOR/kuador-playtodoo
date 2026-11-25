package com.kuador.platodo.api.bookings.infrastructure.events;

import org.springframework.context.ApplicationEventPublisher;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.domain.BookingExpired;

/**
 * Publishes booking events as Spring application events (ADR-007). Listeners that must only react to
 * committed changes use {@code @TransactionalEventListener(phase = AFTER_COMMIT)}.
 */
public class SpringBookingEventPublisher implements BookingEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringBookingEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(BookingExpired event) {
        publisher.publishEvent(event);
    }
}
