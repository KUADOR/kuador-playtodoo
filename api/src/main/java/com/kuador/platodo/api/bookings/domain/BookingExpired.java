package com.kuador.platodo.api.bookings.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A pending booking reached its deadline without being confirmed and released its slot.
 * Carries no personal data (see the event catalog in docs/architecture.md).
 */
public record BookingExpired(
        String tenantId, UUID bookingId, UUID courtId, Instant start, Instant end, BookingKind kind) {

    public BookingExpired {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(bookingId, "bookingId");
        Objects.requireNonNull(courtId, "courtId");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(kind, "kind");
    }

    public static BookingExpired of(String tenantId, Booking booking) {
        return new BookingExpired(tenantId, booking.id(), booking.courtId(),
                booking.period().start(), booking.period().end(), booking.kind());
    }
}
