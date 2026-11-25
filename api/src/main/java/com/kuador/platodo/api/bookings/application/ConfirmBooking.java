package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.event.BookingConfirmed;

/**
 * A player confirms a pending booking before its hold ends. The status change is a conditional update,
 * so it cannot race with the expiry of the hold (D5). Confirming an already confirmed booking is a safe retry.
 */
public class ConfirmBooking {

    private final BookingRepository bookings;
    private final CurrentPlayer currentPlayer;
    private final BookingEventPublisher events;
    private final TransactionOperations transactions;
    private final Clock clock;

    public ConfirmBooking(BookingRepository bookings, CurrentPlayer currentPlayer, BookingEventPublisher events,
            TransactionOperations transactions, Clock clock) {
        this.bookings = bookings;
        this.currentPlayer = currentPlayer;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * @throws BookingNotFoundException   if the booking does not exist or belongs to another player
     * @throws BookingExpiredException    if the hold ended before confirmation
     * @throws BookingNotPendingException if the booking was cancelled
     */
    public Booking confirm(UUID bookingId) {
        UUID playerId = currentPlayer.require();
        Instant now = clock.instant();
        return transactions.execute(transaction -> bookings.confirmIfPending(bookingId, playerId, now)
                .map(this::published)
                .orElseGet(() -> alreadyConfirmed(bookingId, playerId, now)));
    }

    private Booking published(Booking confirmed) {
        events.publish(new BookingConfirmed(confirmed.id(), confirmed.courtId(),
                confirmed.period().start(), confirmed.period().end()));
        return confirmed;
    }

    private Booking alreadyConfirmed(UUID bookingId, UUID playerId, Instant now) {
        Booking booking = OwnBookings.require(bookings, bookingId, playerId).asSeenAt(now);
        return switch (booking.status()) {
            case CONFIRMED -> booking;
            case EXPIRED -> throw new BookingExpiredException(bookingId);
            case PENDING, CANCELLED -> throw new BookingNotPendingException(bookingId, booking.status());
        };
    }
}
