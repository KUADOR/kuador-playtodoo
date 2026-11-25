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
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.event.SlotBlocked;

/**
 * The complex staff makes a free slot unavailable (for example, for maintenance). A block never cancels a
 * booking (criterion 9). The check for an active booking runs under the court write lock that every booking
 * write takes, so a concurrent player booking either commits first and is reported, or waits for the block.
 */
public class BlockSlot {

    /**
     * @param reason optional internal reason, visible only to staff
     */
    public record Command(UUID courtId, Instant start, String reason) {
    }

    private final BookingRepository bookings;
    private final CourtSlots courtSlots;
    private final CurrentStaff currentStaff;
    private final BookingEventPublisher events;
    private final TransactionOperations transactions;
    private final Clock clock;

    public BlockSlot(BookingRepository bookings, CourtSlotRules courtSlotRules, CurrentStaff currentStaff,
            BookingEventPublisher events, TransactionOperations transactions, Clock clock) {
        this.bookings = bookings;
        this.courtSlots = new CourtSlots(courtSlotRules);
        this.currentStaff = currentStaff;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * @throws CourtNotFoundException   if the court does not exist in the active tenant
     * @throws InvalidSlotException     if the start is not a valid future slot of the court
     * @throws SlotHasBookingException  if an active booking occupies the slot; it is left unchanged
     * @throws BookingOverlapException  if another block occupies the slot
     */
    public Booking block(Command command) {
        UUID staffId = currentStaff.require();
        Instant now = clock.instant();
        Booking block = Booking.block(UUID.randomUUID(), command.courtId(),
                courtSlots.slotStartingAt(command.courtId(), command.start(), now), command.reason(), staffId, now);
        return transactions.execute(transaction -> store(block, now));
    }

    private Booking store(Booking block, Instant now) {
        bookings.serializeWritesOfCourt(block.courtId());
        bookings.expireOverdueOverlapping(block.courtId(), block.period(), now);
        bookings.findActiveReservationOverlapping(block.courtId(), block.period(), now).ifPresent(booking -> {
            throw new SlotHasBookingException(booking.id());
        });
        bookings.add(block);
        events.publish(new SlotBlocked(block.id(), block.courtId(), block.period().start(), block.period().end()));
        return block;
    }
}
