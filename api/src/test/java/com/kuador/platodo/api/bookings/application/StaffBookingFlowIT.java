package com.kuador.platodo.api.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingHolder;
import com.kuador.platodo.api.bookings.domain.BookingKind;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.BookingStatus;
import com.kuador.platodo.api.bookings.domain.TimePeriod;
import com.kuador.platodo.api.bookings.domain.event.BookingCancelled;
import com.kuador.platodo.api.bookings.domain.event.BookingCreated;
import com.kuador.platodo.api.bookings.domain.event.SlotBlocked;
import com.kuador.platodo.api.bookings.infrastructure.persistence.PostgresBookingRepository;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.bookings.support.RecordingBookingEvents;
import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.bookings.support.StubCurrentPlayer;
import com.kuador.platodo.api.bookings.support.StubCurrentStaff;
import com.kuador.platodo.api.support.TenantDatabase;

/**
 * Staff booking use cases (manual bookings, blocks and cancellations) against PostgreSQL 18 with the tenant
 * migrations applied. Numbers in test names refer to the acceptance criteria of PLATODO-25.
 */
class StaffBookingFlowIT {

    private static final Instant NOW = Instant.parse("2026-12-04T18:00:00Z");
    private static final Instant EIGHT_PM = Instant.parse("2026-12-04T20:00:00Z");
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration NINETY_MINUTES = Duration.ofMinutes(90);
    private static final BookingHolder HOLDER = new BookingHolder("Holder", null);
    private static final int CONCURRENT_REQUESTS_PER_SIDE = 4;

    private static DataSource tenantA;
    private static DataSource tenantB;

    private final MutableClock clock = new MutableClock(NOW);
    private final StubCourtSlotRules courts = new StubCourtSlotRules();
    private final StubCurrentPlayer currentPlayer = new StubCurrentPlayer();
    private final StubCurrentStaff currentStaff = new StubCurrentStaff();
    private final RecordingBookingEvents events = new RecordingBookingEvents();
    private final UUID staff = UUID.randomUUID();

    private UseCases inA;
    private UseCases inB;
    private UUID court;

    @BeforeAll
    static void migrateTenants() {
        tenantA = TenantDatabase.migratedTenant("tenant_staff_flow_a");
        tenantB = TenantDatabase.migratedTenant("tenant_staff_flow_b");
    }

    /** Calendar queries span all courts of the tenant, so each test starts from empty schemas. */
    @BeforeEach
    void composeUseCasesOnEmptyTenants() {
        List.of(tenantA, tenantB).forEach(tenant -> JdbcClient.create(tenant).sql("DELETE FROM booking").update());
        inA = new UseCases(tenantA);
        inB = new UseCases(tenantB);
        court = courts.addCourt(ONE_HOUR);
        currentStaff.actAs(staff);
        currentPlayer.actAs(UUID.randomUUID());
    }

    @Test
    void aManualBookingIsConfirmedFromTheStartWithTheCourtDuration() {
        UUID longCourt = courts.addCourt(NINETY_MINUTES);

        Booking manual = inA.book(longCourt, EIGHT_PM);

        assertThat(manual.kind()).isEqualTo(BookingKind.RESERVATION);
        assertThat(manual.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(manual.period()).isEqualTo(new TimePeriod(EIGHT_PM, EIGHT_PM.plus(NINETY_MINUTES)));
        assertThat(manual.holder()).isEqualTo(HOLDER);
        assertThat(manual.createdBy()).isEqualTo(staff);
        assertThat(inA.repository.findById(manual.id())).contains(manual);
        assertThat(events.ofType(BookingCreated.class)).containsExactly(new BookingCreated(
                manual.id(), longCourt, EIGHT_PM, EIGHT_PM.plus(NINETY_MINUTES), null));
    }

    @Test
    void aManualBookingFollowsTheSameConflictRuleAsPlayers_criterion8() {
        UUID longCourt = courts.addCourt(NINETY_MINUTES);
        inA.playerBooks(longCourt, EIGHT_PM);

        assertThatThrownBy(() -> inA.book(longCourt, EIGHT_PM.plus(ONE_HOUR)))
                .isInstanceOf(BookingOverlapException.class);
        Booking adjacent = inA.book(longCourt, EIGHT_PM.plus(NINETY_MINUTES));
        assertThatThrownBy(() -> inA.playerBooks(longCourt, EIGHT_PM.plus(Duration.ofHours(2))))
                .isInstanceOf(BookingOverlapException.class);

        assertThat(adjacent.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(events.ofType(BookingCreated.class)).hasSize(2);
    }

    @Test
    void aManualBookingTakesTheSlotOfAnOverduePendingBooking_criterion5() {
        Booking abandoned = inA.playerBooks(court, EIGHT_PM);
        clock.advance(Booking.PENDING_HOLD);

        Booking manual = inA.book(court, EIGHT_PM);

        assertThat(manual.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(inA.repository.findById(abandoned.id())).map(Booking::status).contains(BookingStatus.EXPIRED);
    }

    @Test
    void aManualBookingRejectsAnUnknownCourtAndAnInvalidOrPastStart() {
        assertThatThrownBy(() -> inA.book(UUID.randomUUID(), EIGHT_PM)).isInstanceOf(CourtNotFoundException.class);
        assertThatThrownBy(() -> inA.book(court, EIGHT_PM.plus(Duration.ofMinutes(10))))
                .isInstanceOf(InvalidSlotException.class);
        assertThatThrownBy(() -> inA.book(court, NOW)).isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void blocksAFreeSlotAndAnnouncesIt() {
        Booking block = inA.block(court, EIGHT_PM, "Maintenance");

        assertThat(block.kind()).isEqualTo(BookingKind.BLOCK);
        assertThat(block.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(block.blockReason()).isEqualTo("Maintenance");
        assertThat(block.createdBy()).isEqualTo(staff);
        assertThat(events.ofType(SlotBlocked.class)).containsExactly(
                new SlotBlocked(block.id(), court, EIGHT_PM, EIGHT_PM.plus(ONE_HOUR)));
        assertThatThrownBy(() -> inA.playerBooks(court, EIGHT_PM)).isInstanceOf(BookingOverlapException.class);
    }

    @Test
    void aBlockOnASlotWithABookingIsRejectedAndTheBookingIsKept_criterion9() {
        UUID longCourt = courts.addCourt(NINETY_MINUTES);
        Booking pending = inA.playerBooks(longCourt, EIGHT_PM);
        Booking manual = inA.book(longCourt, EIGHT_PM.plus(NINETY_MINUTES));

        assertThatThrownBy(() -> inA.block(longCourt, EIGHT_PM.plus(ONE_HOUR), null))
                .isInstanceOfSatisfying(SlotHasBookingException.class,
                        rejected -> assertThat(rejected.conflictingBookingId()).isEqualTo(pending.id()));
        assertThatThrownBy(() -> inA.block(longCourt, EIGHT_PM.plus(NINETY_MINUTES), null))
                .isInstanceOfSatisfying(SlotHasBookingException.class,
                        rejected -> assertThat(rejected.conflictingBookingId()).isEqualTo(manual.id()));

        assertThat(inA.repository.findById(pending.id())).contains(pending);
        assertThat(inA.repository.findById(manual.id())).contains(manual);
        assertThat(events.ofType(SlotBlocked.class)).isEmpty();
    }

    @Test
    void aBlockOnABlockedSlotIsUnavailable() {
        inA.block(court, EIGHT_PM, null);

        assertThatThrownBy(() -> inA.block(court, EIGHT_PM, null)).isInstanceOf(BookingOverlapException.class);
    }

    @Test
    void aBlockIsAcceptedOnceTheBookingIsCancelled_criterion10() {
        Booking manual = inA.book(court, EIGHT_PM);
        assertThatThrownBy(() -> inA.block(court, EIGHT_PM, null)).isInstanceOf(SlotHasBookingException.class);

        Booking cancelled = inA.cancel.cancel(manual.id());
        Booking block = inA.block(court, EIGHT_PM, "Maintenance");

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(block.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void aBlockIsAcceptedOnceThePendingBookingIsOverdue_criterion5() {
        inA.playerBooks(court, EIGHT_PM);
        clock.advance(Booking.PENDING_HOLD);

        assertThat(inA.block(court, EIGHT_PM, null).status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancelsAPendingOrConfirmedBookingOrABlockAndReleasesTheSlot() {
        Booking pending = inA.playerBooks(court, EIGHT_PM);
        Booking manual = inA.book(court, EIGHT_PM.plus(ONE_HOUR));
        Booking block = inA.block(court, EIGHT_PM.plus(Duration.ofHours(2)), null);

        List<Booking> cancelled = List.of(
                inA.cancel.cancel(pending.id()), inA.cancel.cancel(manual.id()), inA.cancel.cancel(block.id()));

        assertThat(cancelled).allSatisfy(booking -> {
            assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.cancellation().cancelledBy()).isEqualTo(staff);
            assertThat(booking.cancellation().cancelledAt()).isEqualTo(NOW);
        });
        assertThat(events.ofType(BookingCancelled.class)).extracting(BookingCancelled::bookingId)
                .containsExactly(pending.id(), manual.id(), block.id());
        assertThat(inA.list(NOW, NOW.plus(Duration.ofDays(1)), Optional.of(court))).isEmpty();
        assertThat(inA.book(court, EIGHT_PM).status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancellingAnInactiveBookingReturnsItUnchanged() {
        Booking manual = inA.book(court, EIGHT_PM);
        inA.cancel.cancel(manual.id());
        Booking overdue = inA.playerBooks(court, EIGHT_PM.plus(ONE_HOUR));
        clock.advance(Booking.PENDING_HOLD);

        Booking cancelledAgain = inA.cancel.cancel(manual.id());
        Booking expired = inA.cancel.cancel(overdue.id());

        assertThat(cancelledAgain.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelledAgain.cancellation().cancelledAt()).isEqualTo(NOW);
        assertThat(expired.status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(expired.cancellation()).isNull();
        assertThat(events.ofType(BookingCancelled.class)).hasSize(1);
    }

    @Test
    void cancellingAnUnknownBookingIsNotFound() {
        assertThatThrownBy(() -> inA.cancel.cancel(UUID.randomUUID())).isInstanceOf(BookingNotFoundException.class);
        assertThatThrownBy(() -> inA.get.get(UUID.randomUUID())).isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void readsAnyBookingOfTheComplexWithItsEffectiveStatus() {
        Booking pending = inA.playerBooks(court, EIGHT_PM);
        clock.advance(Booking.PENDING_HOLD);

        assertThat(inA.get.get(pending.id()).status()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void listsActiveBookingsAndBlocksOfTheRangeInOrder() {
        UUID otherCourt = courts.addCourt(ONE_HOUR);
        Booking block = inA.block(otherCourt, EIGHT_PM.minus(ONE_HOUR), "Maintenance");
        Booking manual = inA.book(court, EIGHT_PM.plus(ONE_HOUR));
        Booking pending = inA.playerBooks(court, EIGHT_PM);
        Booking cancelled = inA.book(court, EIGHT_PM.plus(Duration.ofHours(2)));
        inA.cancel.cancel(cancelled.id());
        inA.book(court, NOW.plus(Duration.ofDays(2)));

        List<Booking> all = inA.list(NOW, NOW.plus(Duration.ofDays(1)), Optional.empty());
        List<Booking> ofCourt = inA.list(NOW, NOW.plus(Duration.ofDays(1)), Optional.of(court));

        assertThat(all).extracting(Booking::id).containsExactly(block.id(), pending.id(), manual.id());
        assertThat(ofCourt).extracting(Booking::id).containsExactly(pending.id(), manual.id());
    }

    @Test
    void theListLeavesOutOverduePendingBookings() {
        inA.playerBooks(court, EIGHT_PM);
        clock.advance(Booking.PENDING_HOLD);

        assertThat(inA.list(NOW, NOW.plus(Duration.ofDays(1)), Optional.empty())).isEmpty();
    }

    @Test
    void theListRejectsAnInvertedOrTooLongRange() {
        assertThatThrownBy(() -> inA.list(EIGHT_PM, EIGHT_PM, Optional.empty()))
                .isInstanceOf(InvalidOccupancyRangeException.class);
        assertThatThrownBy(() -> inA.list(NOW, NOW.plus(CalendarRange.MAX_RANGE).plusSeconds(1), Optional.empty()))
                .isInstanceOf(InvalidOccupancyRangeException.class);
    }

    @Test
    void aBlockAndPlayerBookingsRacingForTheSameSlotLeaveExactlyOneActiveEntry() throws Exception {
        List<Callable<Boolean>> requests = new ArrayList<>();
        for (int request = 0; request < CONCURRENT_REQUESTS_PER_SIDE; request++) {
            UUID player = UUID.randomUUID();
            requests.add(() -> {
                currentPlayer.actAs(player);
                return succeeds(() -> inA.playerBooks(court, EIGHT_PM));
            });
            requests.add(() -> {
                currentStaff.actAs(staff);
                return succeeds(() -> inA.block(court, EIGHT_PM, null));
            });
        }

        List<Boolean> outcomes = runAtTheSameTime(requests);

        assertThat(outcomes).hasSize(2 * CONCURRENT_REQUESTS_PER_SIDE).containsOnlyOnce(true);
        assertThat(activeEntriesOfCourt(tenantA, court)).isEqualTo(1);
    }

    @Test
    void concurrentManualBookingsOfTheSameSlotGetExactlyOneBooking_criterion8() throws Exception {
        List<Callable<Boolean>> requests = new ArrayList<>();
        for (int request = 0; request < 2 * CONCURRENT_REQUESTS_PER_SIDE; request++) {
            requests.add(() -> {
                currentStaff.actAs(staff);
                return succeeds(() -> inA.book(court, EIGHT_PM));
            });
        }

        assertThat(runAtTheSameTime(requests)).containsOnlyOnce(true);
        assertThat(activeEntriesOfCourt(tenantA, court)).isEqualTo(1);
    }

    @Test
    void everyStaffUseCaseRequiresStaffOfTheTenant() {
        Booking manual = inA.book(court, EIGHT_PM);

        for (Runnable asSomeoneElse : List.<Runnable>of(currentStaff::actAsOutsider, () -> currentStaff.actAs(null))) {
            asSomeoneElse.run();
            assertThatThrownBy(() -> inA.book(court, EIGHT_PM.plus(ONE_HOUR)))
                    .isInstanceOfAny(StaffNotAuthenticatedException.class, NotStaffOfTenantException.class);
            assertThatThrownBy(() -> inA.block(court, EIGHT_PM.plus(ONE_HOUR), null))
                    .isInstanceOfAny(StaffNotAuthenticatedException.class, NotStaffOfTenantException.class);
            assertThatThrownBy(() -> inA.cancel.cancel(manual.id()))
                    .isInstanceOfAny(StaffNotAuthenticatedException.class, NotStaffOfTenantException.class);
            assertThatThrownBy(() -> inA.get.get(manual.id()))
                    .isInstanceOfAny(StaffNotAuthenticatedException.class, NotStaffOfTenantException.class);
            assertThatThrownBy(() -> inA.list(NOW, EIGHT_PM, Optional.empty()))
                    .isInstanceOfAny(StaffNotAuthenticatedException.class, NotStaffOfTenantException.class);
        }
        assertThat(inA.repository.findById(manual.id())).map(Booking::status).contains(BookingStatus.CONFIRMED);
        assertThat(activeEntriesOfCourt(tenantA, court)).isEqualTo(1);
    }

    @Test
    void staffOfAnotherTenantNeitherSeesNorChangesTheBookingsAndCanUseTheSameSlot_criterion13() {
        Booking manual = inA.book(court, EIGHT_PM);
        Booking block = inA.block(court, EIGHT_PM.plus(ONE_HOUR), null);

        assertThatThrownBy(() -> inB.get.get(manual.id())).isInstanceOf(BookingNotFoundException.class);
        assertThatThrownBy(() -> inB.cancel.cancel(block.id())).isInstanceOf(BookingNotFoundException.class);
        assertThat(inB.list(NOW, NOW.plus(Duration.ofDays(1)), Optional.empty())).isEmpty();

        Booking inTenantB = inB.block(court, EIGHT_PM, null);

        assertThat(inTenantB.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(inA.repository.findById(manual.id())).contains(manual);
        assertThat(inA.repository.findById(block.id())).contains(block);
        assertThat(inA.repository.findById(inTenantB.id())).isEmpty();
        assertThat(activeEntriesOfCourt(tenantA, court)).isEqualTo(2);
        assertThat(activeEntriesOfCourt(tenantB, court)).isEqualTo(1);
    }

    /** The use cases of one tenant, sharing the test doubles of this test. */
    private final class UseCases {

        private final PostgresBookingRepository repository;
        private final CreatePendingBooking playerCreate;
        private final CreateStaffBooking create;
        private final BlockSlot block;
        private final CancelBookingByStaff cancel;
        private final GetStaffBooking get;
        private final ListStaffBookings list;

        private UseCases(DataSource tenant) {
            repository = new PostgresBookingRepository(JdbcClient.create(tenant));
            TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(tenant));
            playerCreate = new CreatePendingBooking(repository, courts, currentPlayer, events, transactions, clock);
            create = new CreateStaffBooking(repository, courts, currentStaff, events, transactions, clock);
            block = new BlockSlot(repository, courts, currentStaff, events, transactions, clock);
            cancel = new CancelBookingByStaff(repository, currentStaff, events, transactions, clock);
            get = new GetStaffBooking(repository, currentStaff, clock);
            list = new ListStaffBookings(repository, currentStaff, clock);
        }

        private Booking book(UUID courtId, Instant start) {
            return create.create(new CreateStaffBooking.Command(courtId, start, HOLDER));
        }

        private Booking block(UUID courtId, Instant start, String reason) {
            return block.block(new BlockSlot.Command(courtId, start, reason));
        }

        private Booking playerBooks(UUID courtId, Instant start) {
            return playerCreate.create(new CreatePendingBooking.Command(courtId, start, true));
        }

        private List<Booking> list(Instant from, Instant to, Optional<UUID> courtId) {
            return list.list(from, to, courtId);
        }
    }

    private static boolean succeeds(Runnable write) {
        try {
            write.run();
            return true;
        } catch (BookingOverlapException | SlotHasBookingException rejected) {
            return false;
        }
    }

    private static <T> List<T> runAtTheSameTime(List<Callable<T>> tasks) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<T> outcomes = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<T>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> {
                        start.await();
                        return task.call();
                    }))
                    .toList();
            start.countDown();
            for (Future<T> future : futures) {
                outcomes.add(future.get());
            }
        }
        return outcomes;
    }

    private static int activeEntriesOfCourt(DataSource tenant, UUID courtId) {
        return JdbcClient.create(tenant)
                .sql("SELECT count(*) FROM booking WHERE court_id = :courtId AND status IN ('PENDING', 'CONFIRMED')")
                .param("courtId", courtId)
                .query(Integer.class)
                .single();
    }
}
