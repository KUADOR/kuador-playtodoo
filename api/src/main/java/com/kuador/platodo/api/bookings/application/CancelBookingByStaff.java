package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CurrentStaff;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.event.BookingCancelled;

/**
 * The complex staff cancels any pending or confirmed booking, or removes a block, releasing the slot.
 * Idempotent: a booking that is already cancelled or expired is returned unchanged, without a new event.
 */
public class CancelBookingByStaff {

    private final BookingRepository bookings;
    private final CurrentStaff currentStaff;
    private final BookingEventPublisher events;
    private final TransactionOperations transactions;
    private final Clock clock;

    public CancelBookingByStaff(BookingRepository bookings, CurrentStaff currentStaff, BookingEventPublisher events,
            TransactionOperations transactions, Clock clock) {
        this.bookings = bookings;
        this.currentStaff = currentStaff;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * @throws BookingNotFoundException if the booking does not exist in the active tenant
     */
    public Booking cancel(UUID bookingId) {
        UUID staffId = currentStaff.require();
        Instant now = clock.instant();
        return transactions.execute(transaction -> bookings.cancelIfActive(bookingId, staffId, now)
                .map(this::published)
                .orElseGet(() -> alreadyInactive(bookingId, now)));
    }

    private Booking published(Booking cancelled) {
        events.publish(new BookingCancelled(cancelled.id(), cancelled.courtId(),
                cancelled.period().start(), cancelled.period().end()));
        return cancelled;
    }

    private Booking alreadyInactive(UUID bookingId, Instant now) {
        return bookings.findById(bookingId)
                .map(booking -> booking.asSeenAt(now))
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }
}
