package com.kuador.platodo.api.bookings.application;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.port.out.CourtSlotRules;
import com.kuador.platodo.api.bookings.domain.TimePeriod;

/**
 * The slot a new booking or block takes: it must start at a valid future slot of the court, and it lasts
 * the court's configured duration at the moment of creation (later changes do not alter it).
 */
final class CourtSlots {

    private final CourtSlotRules rules;

    CourtSlots(CourtSlotRules rules) {
        this.rules = rules;
    }

    /**
     * @throws CourtNotFoundException if the court does not exist in the active tenant
     * @throws InvalidSlotException   if the start is not a valid future slot of the court
     */
    TimePeriod slotStartingAt(UUID courtId, Instant start, Instant now) {
        Duration slotDuration = rules.slotDurationOf(courtId).orElseThrow(() -> new CourtNotFoundException(courtId));
        if (!start.isAfter(now) || !rules.isValidSlotStart(courtId, start)) {
            throw new InvalidSlotException(courtId, start);
        }
        return new TimePeriod(start, start.plus(slotDuration));
    }
}
