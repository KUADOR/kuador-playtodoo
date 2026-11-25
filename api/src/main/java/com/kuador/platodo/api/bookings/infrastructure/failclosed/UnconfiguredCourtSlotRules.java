package com.kuador.platodo.api.bookings.infrastructure.failclosed;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.port.out.CourtSlotRules;

/**
 * Used until the Courts module provides the real slot configuration: every court is reported as not found,
 * so no booking is created with an invented duration.
 */
public class UnconfiguredCourtSlotRules implements CourtSlotRules {

    @Override
    public Optional<Duration> slotDurationOf(UUID courtId) {
        return Optional.empty();
    }

    @Override
    public boolean isValidSlotStart(UUID courtId, Instant start) {
        return false;
    }
}
