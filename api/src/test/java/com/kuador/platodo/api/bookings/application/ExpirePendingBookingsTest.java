package com.kuador.platodo.api.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingExpired;
import com.kuador.platodo.api.bookings.domain.BookingKind;
import com.kuador.platodo.api.bookings.domain.TimePeriod;

class ExpirePendingBookingsTest {

    private static final Instant NOW = Instant.parse("2026-12-04T18:10:00Z");
    private static final TimePeriod PERIOD =
            new TimePeriod(Instant.parse("2026-12-04T20:00:00Z"), Instant.parse("2026-12-04T21:00:00Z"));

    private final StubRepository repository = new StubRepository();
    private final List<BookingExpired> published = new ArrayList<>();
    private final BookingEventPublisher events = event -> published.add((BookingExpired) event);
    private final ExpirePendingBookings expirePendingBookings =
            new ExpirePendingBookings(repository, events, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void expiresWithTheCurrentTimeAndABoundedBatch() {
        expirePendingBookings.expireDue("tenant-a");

        assertThat(repository.requestedNow).isEqualTo(NOW);
        assertThat(repository.requestedLimit).isEqualTo(ExpirePendingBookings.BATCH_SIZE);
    }

    @Test
    void publishesOneEventPerExpiredBookingWithTheTenant() {
        Booking first = pendingCreatedAt(Instant.parse("2026-12-04T18:00:00Z"));
        Booking second = pendingCreatedAt(Instant.parse("2026-12-04T18:01:00Z"));
        repository.dueBookings = List.of(first, second);

        int expired = expirePendingBookings.expireDue("tenant-a");

        assertThat(expired).isEqualTo(2);
        assertThat(published).containsExactly(
                new BookingExpired("tenant-a", first.id(), first.courtId(), PERIOD.start(), PERIOD.end(),
                        BookingKind.RESERVATION),
                new BookingExpired("tenant-a", second.id(), second.courtId(), PERIOD.start(), PERIOD.end(),
                        BookingKind.RESERVATION));
    }

    @Test
    void publishesNothingWhenNoPendingBookingIsDue() {
        assertThat(expirePendingBookings.expireDue("tenant-a")).isZero();
        assertThat(published).isEmpty();
    }

    private static Booking pendingCreatedAt(Instant createdAt) {
        return Booking.pendingReservation(UUID.randomUUID(), UUID.randomUUID(), PERIOD, UUID.randomUUID(), createdAt);
    }

    private static final class StubRepository implements BookingRepository {

        private List<Booking> dueBookings = List.of();
        private Instant requestedNow;
        private int requestedLimit;

        @Override
        public void add(Booking booking) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Booking> findById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void serializeWritesOfCourt(UUID courtId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int expireOverdueOverlapping(UUID courtId, TimePeriod period, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Booking> confirmIfPending(UUID id, UUID playerId, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Booking> cancelIfPending(UUID id, UUID playerId, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Booking> findActiveOfPlayerOverlapping(UUID playerId, TimePeriod period, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TimePeriod> findOccupiedPeriods(UUID courtId, TimePeriod range, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Booking> expirePendingDueBy(Instant now, int limit) {
            requestedNow = now;
            requestedLimit = limit;
            return dueBookings;
        }
    }
}
