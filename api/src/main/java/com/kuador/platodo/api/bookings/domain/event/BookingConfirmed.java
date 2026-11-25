package com.kuador.platodo.api.bookings.domain.event;

import java.time.Instant;
import java.util.UUID;

/** A player confirmed a pending booking within its hold. Carries no personal data. */
public record BookingConfirmed(UUID bookingId, UUID courtId, Instant start, Instant end) implements BookingEvent {
}
