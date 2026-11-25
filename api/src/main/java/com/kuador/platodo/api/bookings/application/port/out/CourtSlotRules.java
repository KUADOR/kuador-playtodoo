package com.kuador.platodo.api.bookings.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Slot configuration of the courts of the active tenant, owned by the Availability module
 * (duration and valid slot starts, set by each complex). Bookings only reads it.
 */
public interface CourtSlotRules {

    /** Slot duration of the court; empty when the court does not exist in the active tenant. */
    Optional<Duration> slotDurationOf(UUID courtId);

    /** Whether {@code start} is the start of a valid slot of the court, per its schedule. */
    boolean isValidSlotStart(UUID courtId, Instant start);
}
