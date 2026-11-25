package com.kuador.platodo.api.bookings.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A reservation or block of one court for a half-open period, inside the schema of one sports complex.
 * Overlaps between active bookings are rejected by the database exclusion constraint (ADR-006);
 * this type keeps the per-row invariants that mirror the table CHECK constraints.
 *
 * @param playerId     player who holds a pending or confirmed reservation; null for staff bookings and blocks
 * @param holder       person a staff manual booking is held for; null otherwise
 * @param blockReason  optional reason of a block
 * @param expiresAt    deadline of a pending booking; required while PENDING
 * @param cancellation set once the booking is cancelled
 */
public record Booking(
        UUID id,
        UUID courtId,
        BookingKind kind,
        BookingStatus status,
        TimePeriod period,
        UUID playerId,
        BookingHolder holder,
        String blockReason,
        UUID createdBy,
        Instant createdAt,
        Instant expiresAt,
        Instant confirmedAt,
        BookingCancellation cancellation) {

    /** How long a player's pending booking holds the slot before it expires (decided by the team). */
    public static final Duration PENDING_HOLD = Duration.ofMinutes(5);

    public Booking {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(courtId, "courtId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt");
        if (status == BookingStatus.PENDING && expiresAt == null) {
            throw new IllegalArgumentException("A pending booking needs an expiry time");
        }
        if (kind == BookingKind.BLOCK && status != BookingStatus.CONFIRMED && status != BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("A block can only be confirmed or cancelled, not " + status);
        }
    }

    /** A player's reservation that holds the slot for {@link #PENDING_HOLD} until it is confirmed. */
    public static Booking pendingReservation(UUID id, UUID courtId, TimePeriod period, UUID playerId, Instant now) {
        Objects.requireNonNull(playerId, "playerId");
        return new Booking(id, courtId, BookingKind.RESERVATION, BookingStatus.PENDING, period,
                playerId, null, null, playerId, now, now.plus(PENDING_HOLD), null, null);
    }

    /** A manual reservation registered by the complex staff; it is confirmed from the start. */
    public static Booking staffReservation(
            UUID id, UUID courtId, TimePeriod period, BookingHolder holder, UUID staffId, Instant now) {
        Objects.requireNonNull(holder, "holder");
        return new Booking(id, courtId, BookingKind.RESERVATION, BookingStatus.CONFIRMED, period,
                null, holder, null, staffId, now, null, now, null);
    }

    /** A slot the complex staff makes unavailable (for example, for maintenance). */
    public static Booking block(UUID id, UUID courtId, TimePeriod period, String reason, UUID staffId, Instant now) {
        return new Booking(id, courtId, BookingKind.BLOCK, BookingStatus.CONFIRMED, period,
                null, null, reason, staffId, now, null, null, null);
    }
}
