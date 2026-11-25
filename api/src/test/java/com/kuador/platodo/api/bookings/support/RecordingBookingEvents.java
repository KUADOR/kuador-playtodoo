package com.kuador.platodo.api.bookings.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.domain.event.BookingEvent;

/** Keeps the published events so tests can assert on them. */
public final class RecordingBookingEvents implements BookingEventPublisher {

    private final List<BookingEvent> published = new CopyOnWriteArrayList<>();

    @Override
    public void publish(BookingEvent event) {
        published.add(event);
    }

    public <T extends BookingEvent> List<T> ofType(Class<T> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
