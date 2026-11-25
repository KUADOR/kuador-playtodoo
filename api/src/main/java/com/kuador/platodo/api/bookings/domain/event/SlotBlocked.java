package com.kuador.platodo.api.bookings.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * The complex staff blocked a free slot of a court. Carries no personal data and no block reason,
 * which is internal to the staff.
 */
public record SlotBlocked(UUID bookingId, UUID courtId, Instant start, Instant end) implements BookingEvent {
}
