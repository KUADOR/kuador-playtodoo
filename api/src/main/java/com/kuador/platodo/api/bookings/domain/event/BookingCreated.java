package com.kuador.platodo.api.bookings.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A reservation was created: a player's pending booking that holds the slot until {@code expiresAt}, or a
 * staff manual booking that is confirmed from the start ({@code expiresAt} is null). Carries no personal data. Consumers must listen synchronously after commit
 * ({@code @TransactionalEventListener(AFTER_COMMIT)}) until events carry the tenant id.
 */
public record BookingCreated(UUID bookingId, UUID courtId, Instant start, Instant end, Instant expiresAt)
        implements BookingEvent {
}
