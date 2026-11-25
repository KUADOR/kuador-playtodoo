package com.kuador.platodo.api.bookings.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class BookingTest {

    private static final Instant NOW = Instant.parse("2026-12-04T18:00:00Z");
    private static final TimePeriod PERIOD =
            new TimePeriod(Instant.parse("2026-12-04T20:00:00Z"), Instant.parse("2026-12-04T21:00:00Z"));

    @Test
    void aPendingReservationHoldsTheSlotForFiveMinutes() {
        UUID playerId = UUID.randomUUID();

        Booking booking = Booking.pendingReservation(UUID.randomUUID(), UUID.randomUUID(), PERIOD, playerId, NOW);

        assertThat(booking.kind()).isEqualTo(BookingKind.RESERVATION);
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.expiresAt()).isEqualTo(Instant.parse("2026-12-04T18:05:00Z"));
        assertThat(booking.playerId()).isEqualTo(playerId);
        assertThat(booking.createdBy()).isEqualTo(playerId);
    }

    @Test
    void aStaffReservationIsConfirmedFromTheStart() {
        Booking booking = Booking.staffReservation(UUID.randomUUID(), UUID.randomUUID(), PERIOD,
                new BookingHolder("Holder", "000"), UUID.randomUUID(), NOW);

        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.confirmedAt()).isEqualTo(NOW);
        assertThat(booking.expiresAt()).isNull();
        assertThat(booking.playerId()).isNull();
    }

    @Test
    void aBlockIsConfirmedFromTheStart() {
        Booking booking = Booking.block(UUID.randomUUID(), UUID.randomUUID(), PERIOD, "Maintenance",
                UUID.randomUUID(), NOW);

        assertThat(booking.kind()).isEqualTo(BookingKind.BLOCK);
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.blockReason()).isEqualTo("Maintenance");
    }

    @Test
    void aPendingBookingNeedsAnExpiryTime() {
        assertThatThrownBy(() -> new Booking(UUID.randomUUID(), UUID.randomUUID(), BookingKind.RESERVATION,
                BookingStatus.PENDING, PERIOD, UUID.randomUUID(), null, null, UUID.randomUUID(), NOW,
                null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aBlockCannotBePending() {
        assertThatThrownBy(() -> new Booking(UUID.randomUUID(), UUID.randomUUID(), BookingKind.BLOCK,
                BookingStatus.PENDING, PERIOD, null, null, null, UUID.randomUUID(), NOW,
                NOW.plus(Booking.PENDING_HOLD), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPendingBookingIsEffectivelyPendingBeforeItsHoldEnds_criterion6() {
        Booking pending = Booking.pendingReservation(UUID.randomUUID(), UUID.randomUUID(), PERIOD,
                UUID.randomUUID(), NOW);
        Instant lastSecondOfHold = pending.expiresAt().minusSeconds(1);

        assertThat(pending.isOverdueAt(lastSecondOfHold)).isFalse();
        assertThat(pending.asSeenAt(lastSecondOfHold)).isSameAs(pending);
    }

    @Test
    void aPendingBookingIsEffectivelyExpiredFromTheEndOfItsHold_criterion6() {
        Booking pending = Booking.pendingReservation(UUID.randomUUID(), UUID.randomUUID(), PERIOD,
                UUID.randomUUID(), NOW);

        Booking seen = pending.asSeenAt(pending.expiresAt());

        assertThat(pending.isOverdueAt(pending.expiresAt())).isTrue();
        assertThat(seen.status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(seen).usingRecursiveComparison().ignoringFields("status").isEqualTo(pending);
    }

    @Test
    void aConfirmedBookingNeverExpires() {
        Booking confirmed = Booking.staffReservation(UUID.randomUUID(), UUID.randomUUID(), PERIOD,
                new BookingHolder("Holder", "000"), UUID.randomUUID(), NOW);

        assertThat(confirmed.asSeenAt(NOW.plus(Booking.PENDING_HOLD).plusSeconds(1))).isSameAs(confirmed);
    }

    @Test
    void aReservationIsHeldOnlyByItsPlayer() {
        UUID playerId = UUID.randomUUID();
        Booking pending = Booking.pendingReservation(UUID.randomUUID(), UUID.randomUUID(), PERIOD, playerId, NOW);

        assertThat(pending.isHeldBy(playerId)).isTrue();
        assertThat(pending.isHeldBy(UUID.randomUUID())).isFalse();
        assertThat(pending.isHeldBy(null)).isFalse();
    }

    @Test
    void theHolderPhoneIsOptional() {
        assertThat(new BookingHolder("Holder", null).phone()).isNull();
    }

    @Test
    void theHolderIsRedactedWhenPrinted() {
        assertThat(new BookingHolder("Holder Name", "000").toString()).doesNotContain("Holder Name", "000");
    }
}
