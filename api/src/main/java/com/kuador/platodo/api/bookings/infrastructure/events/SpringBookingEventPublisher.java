package com.kuador.platodo.api.bookings.infrastructure.events;

import org.springframework.context.ApplicationEventPublisher;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.domain.event.BookingEvent;

/** Publishes booking events as Spring application events (in-process, same thread and transaction). */
public class SpringBookingEventPublisher implements BookingEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringBookingEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(BookingEvent event) {
        publisher.publishEvent(event);
    }
}
