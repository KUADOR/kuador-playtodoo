package com.kuador.platodo.api.bookings.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.kuador.platodo.api.bookings.domain.BookingExpired;
import com.kuador.platodo.api.bookings.domain.BookingKind;

class SpringBookingEventPublisherTest {

    @Test
    void publishesTheEventAsASpringApplicationEvent() {
        List<Object> received = new ArrayList<>();
        ApplicationEventPublisher applicationEvents = received::add;
        BookingExpired event = new BookingExpired("tenant-a", UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-12-04T20:00:00Z"), Instant.parse("2026-12-04T21:00:00Z"),
                BookingKind.RESERVATION);

        new SpringBookingEventPublisher(applicationEvents).publish(event);

        assertThat(received).containsExactly(event);
    }
}
