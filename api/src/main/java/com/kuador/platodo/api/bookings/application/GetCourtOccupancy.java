package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CourtSlotRules;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;
import com.kuador.platodo.api.bookings.domain.TimePeriod;

/**
 * Occupied periods of a court, read from the bookings table as the single source of truth (D4).
 * Kinds and holders are not disclosed.
 */
public class GetCourtOccupancy {

    /** Longest range a client may query at once (API contract). */
    public static final Duration MAX_RANGE = Duration.ofDays(31);

    private final BookingRepository bookings;
    private final CourtSlotRules courtSlotRules;
    private final CurrentPlayer currentPlayer;
    private final Clock clock;

    public GetCourtOccupancy(BookingRepository bookings, CourtSlotRules courtSlotRules,
            CurrentPlayer currentPlayer, Clock clock) {
        this.bookings = bookings;
        this.courtSlotRules = courtSlotRules;
        this.currentPlayer = currentPlayer;
        this.clock = clock;
    }

    /**
     * @throws InvalidOccupancyRangeException if {@code to} is not after {@code from} or the range is too long
     * @throws CourtNotFoundException         if the court does not exist in the active tenant
     */
    public List<TimePeriod> occupiedPeriods(UUID courtId, Instant from, Instant to) {
        currentPlayer.require();
        if (!from.isBefore(to) || Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw new InvalidOccupancyRangeException(from, to);
        }
        if (courtSlotRules.slotDurationOf(courtId).isEmpty()) {
            throw new CourtNotFoundException(courtId);
        }
        return bookings.findOccupiedPeriods(courtId, new TimePeriod(from, to), clock.instant());
    }
}
