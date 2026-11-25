package com.kuador.platodo.api.bookings.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingHolder;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.TimePeriod;
import com.kuador.platodo.api.support.TenantDatabase;

/**
 * Booking persistence against PostgreSQL 18 with the tenant migrations applied.
 * Numbers in test names refer to the acceptance criteria of PLATODO-25.
 */
class PostgresBookingRepositoryIT {

    private static final String TENANT_A = "tenant_bookings_a";
    private static final String TENANT_B = "tenant_bookings_b";
    private static final Instant NOW = Instant.parse("2026-12-04T18:00:00Z");
    private static final TimePeriod EIGHT_TO_NINE = period("20:00", "21:00");
    private static final int CONCURRENT_REQUESTS = 8;

    private static JdbcClient tenantA;
    private static JdbcClient tenantB;
    private static PostgresBookingRepository repositoryA;
    private static PostgresBookingRepository repositoryB;

    private final UUID courtId = UUID.randomUUID();

    @BeforeAll
    static void migrateTenants() {
        tenantA = JdbcClient.create(TenantDatabase.migratedTenant(TENANT_A));
        tenantB = JdbcClient.create(TenantDatabase.migratedTenant(TENANT_B));
        repositoryA = new PostgresBookingRepository(tenantA);
        repositoryB = new PostgresBookingRepository(tenantB);
    }

    @Test
    void storesAndReadsBackAPendingReservation() {
        Booking pending = pending(EIGHT_TO_NINE);

        repositoryA.add(pending);

        assertThat(repositoryA.findById(pending.id())).contains(pending);
    }

    @Test
    void storesAndReadsBackAStaffReservationAndABlock() {
        Booking staffBooking = Booking.staffReservation(UUID.randomUUID(), courtId, EIGHT_TO_NINE,
                new BookingHolder("Holder", "000"), UUID.randomUUID(), NOW);
        Booking block = Booking.block(UUID.randomUUID(), courtId, period("21:00", "22:00"),
                "Maintenance", UUID.randomUUID(), NOW);

        repositoryA.add(staffBooking);
        repositoryA.add(block);

        assertThat(repositoryA.findById(staffBooking.id())).contains(staffBooking);
        assertThat(repositoryA.findById(block.id())).contains(block);
    }

    @Test
    void findByIdOfAnUnknownBookingIsEmpty() {
        assertThat(repositoryA.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void rejectsTheSamePeriodOnTheSameCourt_criterion2() {
        repositoryA.add(pending(EIGHT_TO_NINE));
        Booking duplicate = pending(EIGHT_TO_NINE);

        assertThatThrownBy(() -> repositoryA.add(duplicate))
                .isInstanceOfSatisfying(BookingOverlapException.class, error -> {
                    assertThat(error.courtId()).isEqualTo(courtId);
                    assertThat(error.period()).isEqualTo(EIGHT_TO_NINE);
                });
        assertThat(repositoryA.findById(duplicate.id())).isEmpty();
    }

    @Test
    void rejectsAPartialOverlapWithAnotherDuration_criteria2And11() {
        repositoryA.add(pending(EIGHT_TO_NINE));

        assertThatThrownBy(() -> repositoryA.add(pending(period("20:30", "22:00"))))
                .isInstanceOf(BookingOverlapException.class);
        assertThatThrownBy(() -> repositoryA.add(pending(period("19:30", "20:30"))))
                .isInstanceOf(BookingOverlapException.class);
        assertThatThrownBy(() -> repositoryA.add(pending(period("20:15", "20:45"))))
                .isInstanceOf(BookingOverlapException.class);
        assertThat(activeBookingsOfCourt(tenantA)).isEqualTo(1);
    }

    @Test
    void acceptsAdjacentPeriodsOnTheSameCourt_criteria2And11() {
        repositoryA.add(pending(EIGHT_TO_NINE));

        assertThatNoException().isThrownBy(() -> repositoryA.add(pending(period("21:00", "22:30"))));
        assertThatNoException().isThrownBy(() -> repositoryA.add(pending(period("19:00", "20:00"))));
    }

    @Test
    void acceptsTheSamePeriodOnAnotherCourt() {
        repositoryA.add(pending(EIGHT_TO_NINE));
        Booking otherCourt = Booking.pendingReservation(
                UUID.randomUUID(), UUID.randomUUID(), EIGHT_TO_NINE, UUID.randomUUID(), NOW);

        assertThatNoException().isThrownBy(() -> repositoryA.add(otherCourt));
    }

    @Test
    void rejectsABlockOverAnActiveReservation() {
        repositoryA.add(pending(EIGHT_TO_NINE));
        Booking block = Booking.block(UUID.randomUUID(), courtId, EIGHT_TO_NINE, null, UUID.randomUUID(), NOW);

        assertThatThrownBy(() -> repositoryA.add(block)).isInstanceOf(BookingOverlapException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELLED", "EXPIRED"})
    void aCancelledOrExpiredBookingDoesNotOccupyTheSlot_criterion7(String releasedStatus) {
        Booking released = pending(EIGHT_TO_NINE);
        repositoryA.add(released);
        tenantA.sql("UPDATE booking SET status = :status WHERE id = :id")
                .param("status", releasedStatus)
                .param("id", released.id())
                .update();

        assertThatNoException().isThrownBy(() -> repositoryA.add(pending(EIGHT_TO_NINE)));
        assertThat(activeBookingsOfCourt(tenantA)).isEqualTo(1);
    }

    @Test
    void theDatabaseRejectsADirectDuplicateInsert_criterion14() {
        repositoryA.add(pending(EIGHT_TO_NINE));

        assertThatThrownBy(() -> tenantA.sql("""
                        INSERT INTO booking (id, court_id, kind, status, period, created_by, created_at)
                        VALUES (gen_random_uuid(), :courtId, 'RESERVATION', 'CONFIRMED',
                                '[2026-12-04 20:30+00, 2026-12-04 21:30+00)', gen_random_uuid(), now())
                        """)
                .param("courtId", courtId)
                .update())
                .isInstanceOfSatisfying(DataAccessException.class, error -> assertThat(sqlState(error))
                        .isEqualTo(PostgresBookingRepository.EXCLUSION_VIOLATION));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // pending without expiry
            "'RESERVATION', 'PENDING', '[2026-12-04 20:00+00, 2026-12-04 21:00+00)'",
            // pending block
            "'BLOCK', 'PENDING', '[2026-12-04 20:00+00, 2026-12-04 21:00+00)'",
            // inclusive end
            "'RESERVATION', 'CONFIRMED', '[2026-12-04 20:00+00, 2026-12-04 21:00+00]'",
            // empty period
            "'RESERVATION', 'CONFIRMED', '[2026-12-04 20:00+00, 2026-12-04 20:00+00)'",
            // unknown status
            "'RESERVATION', 'PAID', '[2026-12-04 20:00+00, 2026-12-04 21:00+00)'"})
    void theDatabaseRejectsRowsThatBreakTheBookingChecks(String kindStatusAndPeriod) {
        assertThatThrownBy(() -> tenantA.sql("""
                        INSERT INTO booking (id, court_id, kind, status, period, created_by, created_at)
                        VALUES (gen_random_uuid(), gen_random_uuid(), %s, gen_random_uuid(), now())
                        """.formatted(kindStatusAndPeriod))
                .update())
                .isInstanceOfSatisfying(DataAccessException.class,
                        error -> assertThat(sqlState(error)).isEqualTo("23514"));
    }

    @Test
    void concurrentRequestsForTheSameSlotCreateExactlyOneBooking_criteria1And3() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> requests = new ArrayList<>();
        for (int request = 0; request < CONCURRENT_REQUESTS; request++) {
            requests.add(() -> {
                start.await();
                try {
                    repositoryA.add(pending(EIGHT_TO_NINE));
                    return true;
                } catch (BookingOverlapException | PessimisticLockingFailureException rejected) {
                    // PostgreSQL may resolve conflicting concurrent inserts with a deadlock (40P01)
                    // instead of 23P01; either way the row is not stored. CreatePendingBooking retries it.
                    return false;
                }
            });
        }

        List<Boolean> outcomes = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            List<Future<Boolean>> futures = requests.stream().map(executor::submit).toList();
            start.countDown();
            for (Future<Boolean> future : futures) {
                outcomes.add(future.get());
            }
        }

        assertThat(outcomes).containsOnlyOnce(true).hasSize(CONCURRENT_REQUESTS);
        assertThat(activeBookingsOfCourt(tenantA)).isEqualTo(1);
    }

    @Test
    void anotherTenantWithTheSameCourtAndPeriodIsNotAffected_criterion13() {
        Booking inA = pending(EIGHT_TO_NINE);
        Booking inB = pending(EIGHT_TO_NINE);

        repositoryA.add(inA);
        assertThatNoException().isThrownBy(() -> repositoryB.add(inB));

        assertThat(repositoryA.findById(inB.id())).isEmpty();
        assertThat(repositoryB.findById(inA.id())).isEmpty();
        assertThat(activeBookingsOfCourt(tenantA)).isEqualTo(1);
        assertThat(activeBookingsOfCourt(tenantB)).isEqualTo(1);
    }

    private Booking pending(TimePeriod period) {
        return Booking.pendingReservation(UUID.randomUUID(), courtId, period, UUID.randomUUID(), NOW);
    }

    private int activeBookingsOfCourt(JdbcClient tenant) {
        return tenant.sql("""
                        SELECT count(*) FROM booking
                        WHERE court_id = :courtId AND status IN ('PENDING', 'CONFIRMED')
                        """)
                .param("courtId", courtId)
                .query(Integer.class)
                .single();
    }

    private static TimePeriod period(String from, String to) {
        return new TimePeriod(
                Instant.parse("2026-12-04T" + from + ":00Z"), Instant.parse("2026-12-04T" + to + ":00Z"));
    }

    private static String sqlState(DataAccessException error) {
        return error.getMostSpecificCause() instanceof SQLException sqlError ? sqlError.getSQLState() : null;
    }
}
