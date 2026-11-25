package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.event.BookingCancelled;

/**
 * A player releases one of their pending bookings. Confirmed bookings follow the cancellation policy,
 * which is out of scope, so they are rejected.
 */
public class CancelOwnPendingBooking {

    private final BookingRepository bookings;
    private final CurrentPlayer currentPlayer;
    private final BookingEventPublisher events;
    private final TransactionOperations transactions;
    private final Clock clock;

    public CancelOwnPendingBooking(BookingRepository bookings, CurrentPlayer currentPlayer,
            BookingEventPublisher events, TransactionOperations transactions, Clock clock) {
        this.bookings = bookings;
        this.currentPlayer = currentPlayer;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * @throws BookingNotFoundException   if the booking does not exist or belongs to another player
     * @throws BookingNotPendingException if the booking is confirmed, cancelled or expired
     */
    public Booking cancel(UUID bookingId) {
        UUID playerId = currentPlayer.require();
        Instant now = clock.instant();
        return transactions.execute(transaction -> bookings.cancelIfPending(bookingId, playerId, now)
                .map(this::published)
                .orElseThrow(() -> notPending(bookingId, playerId, now)));
    }

    private Booking published(Booking cancelled) {
        events.publish(new BookingCancelled(cancelled.id(), cancelled.courtId(),
                cancelled.period().start(), cancelled.period().end()));
        return cancelled;
    }

    private RuntimeException notPending(UUID bookingId, UUID playerId, Instant now) {
        Booking booking = OwnBookings.require(bookings, bookingId, playerId).asSeenAt(now);
        return new BookingNotPendingException(bookingId, booking.status());
    }
}
