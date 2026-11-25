package com.kuador.platodo.api.bookings.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.TimePeriod;

/**
 * Bookings of the tenant whose schema is active on the current connection.
 * Implementations never read or write another tenant's schema.
 */
public interface BookingRepository {

    /**
     * Stores a new booking.
     *
     * @throws BookingOverlapException if it is active and overlaps another active booking of the same court
     */
    void add(Booking booking);

    Optional<Booking> findById(UUID id);

    /**
     * Makes the writes on the court's slots wait for each other until the current transaction ends.
     * Without it, concurrent conflicting inserts may fail with a database deadlock instead of a clean
     * {@link BookingOverlapException}. It does not replace the no-overlap constraint, which stays the guarantee.
     * Must run inside a transaction.
     */
    void serializeWritesOfCourt(UUID courtId);

    /**
     * Marks as EXPIRED the pending bookings of the court that overlap {@code period} and whose hold ended
     * at or before {@code now}, so they stop occupying the slot (lazy expiry, ADR-006).
     *
     * @return how many bookings expired
     */
    int expireOverdueOverlapping(UUID courtId, TimePeriod period, Instant now);

    /**
     * Confirms the player's booking only if it is still pending and its hold ends after {@code now}.
     *
     * @return the confirmed booking, or empty if no booking matched those conditions
     */
    Optional<Booking> confirmIfPending(UUID id, UUID playerId, Instant now);

    /**
     * Cancels the player's booking only if it is still pending and its hold ends after {@code now}.
     *
     * @return the cancelled booking, or empty if no booking matched those conditions
     */
    Optional<Booking> cancelIfPending(UUID id, UUID playerId, Instant now);

    /**
     * Cancels the booking or block whatever its holder, only if it still occupies its slot at {@code now}
     * (confirmed, or pending and not overdue).
     *
     * @return the cancelled booking, or empty if no booking matched those conditions
     */
    Optional<Booking> cancelIfActive(UUID id, UUID cancelledBy, Instant now);

    /**
     * An active reservation of the court (confirmed, or pending and not overdue) that overlaps {@code period};
     * blocks are not considered.
     */
    Optional<Booking> findActiveReservationOverlapping(UUID courtId, TimePeriod period, Instant now);

    /**
     * Reservations and blocks of all courts that occupy their slot at {@code now} and overlap {@code range};
     * ordered by start.
     */
    List<Booking> findActiveOverlapping(TimePeriod range, Instant now);

    /** Like {@link #findActiveOverlapping}, restricted to one court. */
    List<Booking> findActiveOfCourtOverlapping(UUID courtId, TimePeriod range, Instant now);

    /** An active booking of the player (confirmed, or pending and not overdue) that overlaps {@code period}. */
    Optional<Booking> findActiveOfPlayerOverlapping(UUID playerId, TimePeriod period, Instant now);

    /**
     * Periods of the court taken by a confirmed booking or block, or by a pending booking not overdue at
     * {@code now}, that overlap {@code range}; ordered by start.
     */
    List<TimePeriod> findOccupiedPeriods(UUID courtId, TimePeriod range, Instant now);

    /**
     * Marks as EXPIRED, in one atomic step, up to {@code limit} pending bookings whose deadline is at or
     * before {@code now}, which frees their slots. Rows another caller is changing at the same moment are
     * left to that caller, so each booking is returned by exactly one caller.
     *
     * @return the bookings this call expired, already with status EXPIRED
     */
    List<Booking> expirePendingDueBy(Instant now, int limit);
}
