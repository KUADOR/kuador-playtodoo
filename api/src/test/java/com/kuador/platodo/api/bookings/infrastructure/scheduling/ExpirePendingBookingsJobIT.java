package com.kuador.platodo.api.bookings.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.bookings.application.ExpirePendingBookings;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingExpired;
import com.kuador.platodo.api.bookings.domain.BookingHolder;
import com.kuador.platodo.api.bookings.domain.BookingStatus;
import com.kuador.platodo.api.bookings.domain.TimePeriod;
import com.kuador.platodo.api.bookings.infrastructure.persistence.PostgresBookingRepository;
import com.kuador.platodo.api.support.SchemaSwitchingTenants;
import com.kuador.platodo.api.support.TenantDatabase;

/**
 * Scheduled expiry of pending bookings against PostgreSQL 18 with real tenant schemas.
 * Numbers in test names refer to the acceptance criteria of PLATODO-25.
 */
class ExpirePendingBookingsJobIT {

    private static final String TENANT_A = "tenant_expiry_a";
    private static final String TENANT_B = "tenant_expiry_b";
    /** Exists but is not active (for example, suspended): the job must not touch it. */
    private static final String INACTIVE_TENANT = "tenant_expiry_inactive";

    private static final Instant CREATED_AT = Instant.parse("2026-12-04T18:00:00Z");
    private static final Instant AFTER_DEADLINE = CREATED_AT.plus(Booking.PENDING_HOLD).plusSeconds(1);
    private static final TimePeriod EIGHT_TO_NINE =
            new TimePeriod(Instant.parse("2026-12-04T20:00:00Z"), Instant.parse("2026-12-04T21:00:00Z"));
    private static final int RACE_ROUNDS = 20;
    private static final int DUE_BOOKINGS = 30;

    private static JdbcClient tenantA;
    private static JdbcClient tenantB;
    private static JdbcClient inactiveTenant;
    private static PostgresBookingRepository repositoryA;
    private static PostgresBookingRepository repositoryB;
    private static PostgresBookingRepository inactiveRepository;
    private static SchemaSwitchingTenants activeTenants;

    private final List<BookingExpired> published = new CopyOnWriteArrayList<>();
    private final UUID courtId = UUID.randomUUID();

    @BeforeAll
    static void migrateTenants() {
        tenantA = JdbcClient.create(TenantDatabase.migratedTenant(TENANT_A));
        tenantB = JdbcClient.create(TenantDatabase.migratedTenant(TENANT_B));
        inactiveTenant = JdbcClient.create(TenantDatabase.migratedTenant(INACTIVE_TENANT));
        repositoryA = new PostgresBookingRepository(tenantA);
        repositoryB = new PostgresBookingRepository(tenantB);
        inactiveRepository = new PostgresBookingRepository(inactiveTenant);
        activeTenants = new SchemaSwitchingTenants(List.of(TENANT_A, TENANT_B));
    }

    @BeforeEach
    void emptyTenants() {
        for (JdbcClient tenant : List.of(tenantA, tenantB, inactiveTenant)) {
            tenant.sql("DELETE FROM booking").update();
        }
    }

    @Test
    void aDuePendingBookingExpiresAndFreesItsSlot_criteria5And7() {
        Booking pending = pendingCreatedAt(CREATED_AT);
        repositoryA.add(pending);

        jobAt(AFTER_DEADLINE).run();

        assertThat(statusIn(tenantA, pending.id())).isEqualTo(BookingStatus.EXPIRED);
        assertThat(published).containsExactly(BookingExpired.of(TENANT_A, pending));
        assertThatNoException().isThrownBy(() -> repositoryA.add(pendingCreatedAt(AFTER_DEADLINE)));
    }

    @Test
    void aPendingBookingExpiresExactlyAtItsDeadline() {
        Booking pending = pendingCreatedAt(CREATED_AT);
        repositoryA.add(pending);

        jobAt(pending.expiresAt()).run();

        assertThat(statusIn(tenantA, pending.id())).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void keepsPendingBookingsBeforeTheirDeadlineAndConfirmedOnes() {
        Booking stillHeld = pendingCreatedAt(AFTER_DEADLINE.minus(Duration.ofMinutes(1)));
        Booking confirmed = Booking.staffReservation(UUID.randomUUID(), courtId,
                new TimePeriod(EIGHT_TO_NINE.end(), EIGHT_TO_NINE.end().plus(Duration.ofHours(1))),
                new BookingHolder("Holder", "000"), UUID.randomUUID(), CREATED_AT);
        repositoryA.add(stillHeld);
        repositoryA.add(confirmed);

        jobAt(AFTER_DEADLINE).run();

        assertThat(statusIn(tenantA, stillHeld.id())).isEqualTo(BookingStatus.PENDING);
        assertThat(statusIn(tenantA, confirmed.id())).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(published).isEmpty();
    }

    @Test
    void runningAgainExpiresNothingTwice() {
        repositoryA.add(pendingCreatedAt(CREATED_AT));
        ExpirePendingBookingsJob job = jobAt(AFTER_DEADLINE);

        job.run();
        job.run();

        assertThat(published).hasSize(1);
    }

    @Test
    void twoInstancesRunningAtOnceExpireEachBookingOnce() throws Exception {
        for (int booking = 0; booking < DUE_BOOKINGS; booking++) {
            repositoryA.add(Booking.pendingReservation(
                    UUID.randomUUID(), UUID.randomUUID(), EIGHT_TO_NINE, UUID.randomUUID(), CREATED_AT));
        }
        ExpirePendingBookingsJob firstInstance = jobAt(AFTER_DEADLINE);
        ExpirePendingBookingsJob secondInstance = jobAt(AFTER_DEADLINE);

        runTogether(List.of(
                () -> {
                    firstInstance.run();
                    return true;
                },
                () -> {
                    secondInstance.run();
                    return true;
                }));

        assertThat(published).hasSize(DUE_BOOKINGS);
        assertThat(published.stream().map(BookingExpired::bookingId).distinct()).hasSize(DUE_BOOKINGS);
        assertThat(countIn(tenantA, BookingStatus.EXPIRED)).isEqualTo(DUE_BOOKINGS);
    }

    @Test
    void confirmationRacingExpiryLeavesASingleFinalState() throws Exception {
        Instant confirmerClock = CREATED_AT.plus(Booking.PENDING_HOLD).minusSeconds(1);
        for (int round = 0; round < RACE_ROUNDS; round++) {
            Booking pending = Booking.pendingReservation(
                    UUID.randomUUID(), UUID.randomUUID(), EIGHT_TO_NINE, UUID.randomUUID(), CREATED_AT);
            repositoryA.add(pending);

            List<Boolean> outcomes = runTogether(List.of(
                    () -> confirm(pending, confirmerClock),
                    () -> repositoryA.expirePendingDueBy(AFTER_DEADLINE, ExpirePendingBookings.BATCH_SIZE)
                            .stream().anyMatch(expired -> expired.id().equals(pending.id()))));

            boolean confirmed = outcomes.get(0);
            boolean expired = outcomes.get(1);
            assertThat(confirmed).as("round %d: exactly one transition wins", round).isNotEqualTo(expired);
            assertThat(statusIn(tenantA, pending.id()))
                    .isEqualTo(confirmed ? BookingStatus.CONFIRMED : BookingStatus.EXPIRED);
        }
    }

    @Test
    void expiresEachActiveTenantInItsOwnSchemaOnly_criterion13() {
        Booking inA = pendingCreatedAt(CREATED_AT);
        Booking inB = pendingCreatedAt(CREATED_AT);
        Booking inInactive = pendingCreatedAt(CREATED_AT);
        repositoryA.add(inA);
        repositoryB.add(inB);
        inactiveRepository.add(inInactive);

        jobAt(AFTER_DEADLINE).run();

        assertThat(published).containsExactlyInAnyOrder(
                BookingExpired.of(TENANT_A, inA), BookingExpired.of(TENANT_B, inB));
        assertThat(statusIn(tenantA, inA.id())).isEqualTo(BookingStatus.EXPIRED);
        assertThat(statusIn(tenantB, inB.id())).isEqualTo(BookingStatus.EXPIRED);
        assertThat(statusIn(inactiveTenant, inInactive.id())).isEqualTo(BookingStatus.PENDING);
        assertThat(repositoryA.findById(inB.id())).isEmpty();
        assertThat(repositoryB.findById(inA.id())).isEmpty();
    }

    private ExpirePendingBookingsJob jobAt(Instant now) {
        PostgresBookingRepository currentTenantBookings =
                new PostgresBookingRepository(JdbcClient.create(activeTenants.dataSource()));
        ExpirePendingBookings expirePendingBookings = new ExpirePendingBookings(
                currentTenantBookings, event -> published.add((BookingExpired) event), Clock.fixed(now, ZoneOffset.UTC));
        return new ExpirePendingBookingsJob(activeTenants, expirePendingBookings);
    }

    private Booking pendingCreatedAt(Instant createdAt) {
        return Booking.pendingReservation(UUID.randomUUID(), courtId, EIGHT_TO_NINE, UUID.randomUUID(), createdAt);
    }

    /** The conditional confirmation of ADR-006 (D5), owned by PLATODO-31; here it only plays the other side. */
    private static boolean confirm(Booking pending, Instant now) {
        return tenantA.sql("""
                        UPDATE booking SET status = 'CONFIRMED', confirmed_at = :now
                        WHERE id = :id AND player_id = :playerId AND status = 'PENDING' AND expires_at > :now
                        """)
                .param("now", now.atOffset(ZoneOffset.UTC))
                .param("id", pending.id())
                .param("playerId", pending.playerId())
                .update() == 1;
    }

    private static List<Boolean> runTogether(List<Callable<Boolean>> tasks) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<Boolean>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> {
                        start.await();
                        return task.call();
                    }))
                    .toList();
            start.countDown();
            List<Boolean> outcomes = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                outcomes.add(future.get());
            }
            return outcomes;
        }
    }

    private static BookingStatus statusIn(JdbcClient tenant, UUID bookingId) {
        return tenant.sql("SELECT status FROM booking WHERE id = :id")
                .param("id", bookingId)
                .query((row, rowNumber) -> BookingStatus.valueOf(row.getString("status")))
                .single();
    }

    private static int countIn(JdbcClient tenant, BookingStatus status) {
        return tenant.sql("SELECT count(*) FROM booking WHERE status = :status")
                .param("status", status.name())
                .query(Integer.class)
                .single();
    }
}
