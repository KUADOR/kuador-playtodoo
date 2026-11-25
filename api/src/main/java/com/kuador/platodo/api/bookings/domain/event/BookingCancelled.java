package com.kuador.platodo.api.bookings.domain.event;

import java.time.Instant;
import java.util.UUID;

/** A booking was cancelled and its slot released. Carries no personal data. */
public record BookingCancelled(UUID bookingId, UUID courtId, Instant start, Instant end) implements BookingEvent {
}
