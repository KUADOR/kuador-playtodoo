package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CourtSlotRules;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.TimePeriod;
import com.kuador.platodo.api.bookings.domain.event.BookingCreated;

/**
 * A player holds a court slot for {@link Booking#PENDING_HOLD} until confirming it. The end of the slot comes
 * from the court's configured duration; the no-overlap guarantee comes from the database (ADR-006).
 */
public class CreatePendingBooking {

    /**
     * @param acknowledgeOwnOverlap the player accepted the warning about an overlapping booking of their own
     */
    public record Command(UUID courtId, Instant start, boolean acknowledgeOwnOverlap) {
    }

    private final BookingRepository bookings;
    private final CourtSlotRules courtSlotRules;
    private final CurrentPlayer currentPlayer;
    private final BookingEventPublisher events;
    private final TransactionOperations transactions;
    private final Clock clock;

    public CreatePendingBooking(BookingRepository bookings, CourtSlotRules courtSlotRules,
            CurrentPlayer currentPlayer, BookingEventPublisher events, TransactionOperations transactions,
            Clock clock) {
        this.bookings = bookings;
        this.courtSlotRules = courtSlotRules;
        this.currentPlayer = currentPlayer;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * @throws CourtNotFoundException               if the court does not exist in the active tenant
     * @throws InvalidSlotException                 if the start is not a valid future slot of the court
     * @throws OwnOverlapNeedsConfirmationException if the player holds an overlapping booking and did not
     *                                              acknowledge it
     * @throws BookingOverlapException              if the slot is taken by another active booking or block
     */
    public Booking create(Command command) {
        UUID playerId = currentPlayer.require();
        Instant now = clock.instant();
        TimePeriod period = slotOf(command.courtId(), command.start(), now);
        if (!command.acknowledgeOwnOverlap()) {
            rejectOwnOverlap(playerId, period, now);
        }
        Booking pending = Booking.pendingReservation(UUID.randomUUID(), command.courtId(), period, playerId, now);
        return transactions.execute(transaction -> store(pending, now));
    }

    private Booking store(Booking pending, Instant now) {
        bookings.serializeWritesOfCourt(pending.courtId());
        bookings.expireOverdueOverlapping(pending.courtId(), pending.period(), now);
        bookings.add(pending);
        events.publish(new BookingCreated(pending.id(), pending.courtId(), pending.period().start(),
                pending.period().end(), pending.expiresAt()));
        return pending;
    }

    private TimePeriod slotOf(UUID courtId, Instant start, Instant now) {
        Duration slotDuration = courtSlotRules.slotDurationOf(courtId)
                .orElseThrow(() -> new CourtNotFoundException(courtId));
        if (!start.isAfter(now) || !courtSlotRules.isValidSlotStart(courtId, start)) {
            throw new InvalidSlotException(courtId, start);
        }
        return new TimePeriod(start, start.plus(slotDuration));
    }

    /** Only the active tenant is checked: looking at other complexes would cross schemas (D6). */
    private void rejectOwnOverlap(UUID playerId, TimePeriod period, Instant now) {
        bookings.findActiveOfPlayerOverlapping(playerId, period, now).ifPresent(existing -> {
            throw new OwnOverlapNeedsConfirmationException(existing.asSeenAt(now));
        });
    }
}
