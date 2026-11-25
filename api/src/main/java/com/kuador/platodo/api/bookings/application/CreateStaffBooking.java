package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CourtSlotRules;
import com.kuador.platodo.api.bookings.application.port.out.CurrentStaff;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingHolder;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.event.BookingCreated;

/**
 * The complex staff registers a booking for a customer (for example, taken by phone). It is confirmed from
 * the start (D7) and follows the same no-overlap rule as player bookings (criterion 8).
 */
public class CreateStaffBooking {

    public record Command(UUID courtId, Instant start, BookingHolder holder) {
    }

    private final BookingRepository bookings;
    private final CourtSlots courtSlots;
    private final CurrentStaff currentStaff;
    private final BookingEventPublisher events;
    private final TransactionOperations transactions;
    private final Clock clock;

    public CreateStaffBooking(BookingRepository bookings, CourtSlotRules courtSlotRules, CurrentStaff currentStaff,
            BookingEventPublisher events, TransactionOperations transactions, Clock clock) {
        this.bookings = bookings;
        this.courtSlots = new CourtSlots(courtSlotRules);
        this.currentStaff = currentStaff;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * @throws CourtNotFoundException  if the court does not exist in the active tenant
     * @throws InvalidSlotException    if the start is not a valid future slot of the court
     * @throws BookingOverlapException if the slot is taken by another active booking or block
     */
    public Booking create(Command command) {
        UUID staffId = currentStaff.require();
        Instant now = clock.instant();
        Booking booking = Booking.staffReservation(UUID.randomUUID(), command.courtId(),
                courtSlots.slotStartingAt(command.courtId(), command.start(), now), command.holder(), staffId, now);
        return transactions.execute(transaction -> store(booking, now));
    }

    private Booking store(Booking booking, Instant now) {
        bookings.serializeWritesOfCourt(booking.courtId());
        bookings.expireOverdueOverlapping(booking.courtId(), booking.period(), now);
        bookings.add(booking);
        events.publish(new BookingCreated(booking.id(), booking.courtId(), booking.period().start(),
                booking.period().end(), booking.expiresAt()));
        return booking;
    }
}
