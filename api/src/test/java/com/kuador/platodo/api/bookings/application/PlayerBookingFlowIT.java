package com.kuador.platodo.api.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.BookingStatus;
import com.kuador.platodo.api.bookings.domain.TimePeriod;
import com.kuador.platodo.api.bookings.domain.event.BookingCancelled;
import com.kuador.platodo.api.bookings.domain.event.BookingConfirmed;
import com.kuador.platodo.api.bookings.domain.event.BookingCreated;
import com.kuador.platodo.api.bookings.infrastructure.persistence.PostgresBookingRepository;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.bookings.support.RecordingBookingEvents;
import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.bookings.support.StubCurrentPlayer;
import com.kuador.platodo.api.support.TenantDatabase;

/**
 * Player booking use cases against PostgreSQL 18 with the tenant migrations applied. Each tenant has its own
 * data source whose search_path is that tenant's schema, as Tenancy will set it per request.
 * Numbers in test names refer to the acceptance criteria of PLATODO-25.
 */
class PlayerBookingFlowIT {

    private static final Instant NOW = Instant.parse("2026-12-04T18:00:00Z");
    private static final Instant EIGHT_PM = Instant.parse("2026-12-04T20:00:00Z");
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration NINETY_MINUTES = Duration.ofMinutes(90);
    private static final int CONCURRENT_REQUESTS = 8;

    private static DataSource tenantA;
    private static DataSource tenantB;

    private final MutableClock clock = new MutableClock(NOW);
    private final StubCourtSlotRules courts = new StubCourtSlotRules();
    private final StubCurrentPlayer currentPlayer = new StubCurrentPlayer();
    private final RecordingBookingEvents events = new RecordingBookingEvents();
    private final UUID player = UUID.randomUUID();

    private UseCases inA;
    private UseCases inB;
    private UUID court;

    @BeforeAll
    static void migrateTenants() {
        tenantA = TenantDatabase.migratedTenant("tenant_player_flow_a");
        tenantB = TenantDatabase.migratedTenant("tenant_player_flow_b");
    }

    @BeforeEach
    void composeUseCases() {
        inA = new UseCases(tenantA);
        inB = new UseCases(tenantB);
        court = courts.addCourt(ONE_HOUR);
        currentPlayer.actAs(player);
    }

    @Test
    void createsAPendingBookingThatHoldsTheSlotForFiveMinutes() {
        Booking pending = inA.create(court, EIGHT_PM);

        assertThat(pending.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(pending.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(pending.playerId()).isEqualTo(player);
        assertThat(inA.repository.findById(pending.id())).contains(pending);
        assertThat(events.ofType(BookingCreated.class)).containsExactly(new BookingCreated(
                pending.id(), court, EIGHT_PM, EIGHT_PM.plus(ONE_HOUR), pending.expiresAt()));
    }

    @Test
    void theEndOfTheBookingComesFromTheDurationOfEachCourt_criterion11() {
        UUID longCourt = courts.addCourt(NINETY_MINUTES);

        Booking oneHour = inA.create(court, EIGHT_PM);
        Booking ninetyMinutes = inA.createAcknowledgingOverlap(longCourt, EIGHT_PM);

        assertThat(oneHour.period()).isEqualTo(new TimePeriod(EIGHT_PM, EIGHT_PM.plus(ONE_HOUR)));
        assertThat(ninetyMinutes.period()).isEqualTo(new TimePeriod(EIGHT_PM, EIGHT_PM.plus(NINETY_MINUTES)));
    }

    @Test
    void aBookingThatOverlapsPartiallyAnotherOneOfTheSameCourtIsRejected_criteria2And11() {
        UUID longCourt = courts.addCourt(NINETY_MINUTES);
        inA.create(longCourt, EIGHT_PM);
        currentPlayer.actAs(UUID.randomUUID());

        assertThatThrownBy(() -> inA.create(longCourt, EIGHT_PM.plus(Duration.ofMinutes(60))))
                .isInstanceOf(BookingOverlapException.class);
        assertThat(inA.create(longCourt, EIGHT_PM.plus(NINETY_MINUTES)).status()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void rejectsACourtThatDoesNotExistInTheTenant() {
        assertThatThrownBy(() -> inA.create(UUID.randomUUID(), EIGHT_PM))
                .isInstanceOf(CourtNotFoundException.class);
    }

    @Test
    void rejectsAStartThatIsNotAValidSlotOfTheCourt() {
        assertThatThrownBy(() -> inA.create(court, EIGHT_PM.plus(Duration.ofMinutes(10))))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void rejectsAStartInThePast() {
        assertThatThrownBy(() -> inA.create(court, NOW.minus(ONE_HOUR)))
                .isInstanceOf(InvalidSlotException.class);
        assertThatThrownBy(() -> inA.create(court, NOW))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void concurrentPlayersRequestingTheSameSlotGetExactlyOneBooking_criteria1And3() throws Exception {
        List<Callable<Boolean>> requests = new ArrayList<>();
        for (int request = 0; request < CONCURRENT_REQUESTS; request++) {
            UUID otherPlayer = UUID.randomUUID();
            requests.add(() -> {
                currentPlayer.actAs(otherPlayer);
                try {
                    inA.create(court, EIGHT_PM);
                    return true;
                } catch (BookingOverlapException rejected) {
                    return false;
                }
            });
        }

        List<Boolean> outcomes = runAtTheSameTime(requests);

        assertThat(outcomes).hasSize(CONCURRENT_REQUESTS).containsOnlyOnce(true);
        assertThat(activeBookingsOfCourt(tenantA)).isEqualTo(1);
        assertThat(events.ofType(BookingCreated.class)).hasSize(1);
    }

    @Test
    void anOverduePendingBookingNoLongerBlocksTheSlot_criterion5() {
        Booking abandoned = inA.create(court, EIGHT_PM);
        clock.advance(Booking.PENDING_HOLD);
        currentPlayer.actAs(UUID.randomUUID());

        Booking taken = inA.create(court, EIGHT_PM);

        assertThat(taken.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(storedStatus(tenantA, abandoned.id())).isEqualTo("EXPIRED");
        assertThat(activeBookingsOfCourt(tenantA)).isEqualTo(1);
    }

    @Test
    void confirmsWithinTheHold_criterion6() {
        Booking pending = inA.create(court, EIGHT_PM);
        clock.advance(Booking.PENDING_HOLD.minusSeconds(1));

        Booking confirmed = inA.confirm.confirm(pending.id());

        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.confirmedAt()).isEqualTo(clock.instant());
        assertThat(storedStatus(tenantA, pending.id())).isEqualTo("CONFIRMED");
        assertThat(events.ofType(BookingConfirmed.class)).containsExactly(new BookingConfirmed(
                pending.id(), court, EIGHT_PM, EIGHT_PM.plus(ONE_HOUR)));
    }

    @Test
    void rejectsConfirmingOnceTheHoldEnded_criterion6() {
        Booking pending = inA.create(court, EIGHT_PM);
        clock.advance(Booking.PENDING_HOLD);

        assertThatThrownBy(() -> inA.confirm.confirm(pending.id())).isInstanceOf(BookingExpiredException.class);
        assertThat(inA.get.get(pending.id()).status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(events.ofType(BookingConfirmed.class)).isEmpty();
    }

    @Test
    void confirmingTwiceIsASafeRetryWithASingleEvent() {
        Booking pending = inA.create(court, EIGHT_PM);
        inA.confirm.confirm(pending.id());

        Booking again = inA.confirm.confirm(pending.id());

        assertThat(again.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(events.ofType(BookingConfirmed.class)).hasSize(1);
    }

    @Test
    void concurrentConfirmationsConfirmOnceAndPublishOneEvent() throws Exception {
        Booking pending = inA.create(court, EIGHT_PM);
        List<Callable<BookingStatus>> confirmations = new ArrayList<>();
        for (int request = 0; request < CONCURRENT_REQUESTS; request++) {
            confirmations.add(() -> {
                currentPlayer.actAs(player);
                return inA.confirm.confirm(pending.id()).status();
            });
        }

        List<BookingStatus> outcomes = runAtTheSameTime(confirmations);

        assertThat(outcomes).hasSize(CONCURRENT_REQUESTS).containsOnly(BookingStatus.CONFIRMED);
        assertThat(events.ofType(BookingConfirmed.class)).hasSize(1);
    }

    @Test
    void rejectsConfirmingACancelledBooking() {
        Booking pending = inA.create(court, EIGHT_PM);
        inA.cancel.cancel(pending.id());

        assertThatThrownBy(() -> inA.confirm.confirm(pending.id()))
                .isInstanceOf(BookingNotPendingException.class);
    }

    @Test
    void anotherPlayerCannotSeeConfirmOrCancelTheBooking() {
        Booking pending = inA.create(court, EIGHT_PM);
        currentPlayer.actAs(UUID.randomUUID());

        assertThatThrownBy(() -> inA.get.get(pending.id())).isInstanceOf(BookingNotFoundException.class);
        assertThatThrownBy(() -> inA.confirm.confirm(pending.id())).isInstanceOf(BookingNotFoundException.class);
        assertThatThrownBy(() -> inA.cancel.cancel(pending.id())).isInstanceOf(BookingNotFoundException.class);
        assertThat(storedStatus(tenantA, pending.id())).isEqualTo("PENDING");
    }

    @Test
    void cancellingOwnPendingBookingReleasesTheSlot_criterion7() {
        Booking pending = inA.create(court, EIGHT_PM);

        Booking cancelled = inA.cancel.cancel(pending.id());
        currentPlayer.actAs(UUID.randomUUID());
        Booking next = inA.create(court, EIGHT_PM);

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.cancellation().cancelledBy()).isEqualTo(player);
        assertThat(next.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(events.ofType(BookingCancelled.class)).containsExactly(new BookingCancelled(
                pending.id(), court, EIGHT_PM, EIGHT_PM.plus(ONE_HOUR)));
    }

    @Test
    void rejectsCancellingAConfirmedOrExpiredBooking() {
        Booking confirmed = inA.create(court, EIGHT_PM);
        inA.confirm.confirm(confirmed.id());
        Booking expired = inA.createAcknowledgingOverlap(courts.addCourt(ONE_HOUR), EIGHT_PM);
        clock.advance(Booking.PENDING_HOLD);

        assertThatThrownBy(() -> inA.cancel.cancel(confirmed.id())).isInstanceOf(BookingNotPendingException.class);
        assertThatThrownBy(() -> inA.cancel.cancel(expired.id())).isInstanceOf(BookingNotPendingException.class);
        assertThat(events.ofType(BookingCancelled.class)).isEmpty();
    }

    @Test
    void warnsWhenThePlayerAlreadyHoldsAnOverlappingBookingInTheComplex_criterion12() {
        Booking first = inA.create(court, EIGHT_PM);
        UUID otherCourt = courts.addCourt(NINETY_MINUTES);

        assertThatThrownBy(() -> inA.create(otherCourt, EIGHT_PM.minus(Duration.ofMinutes(30))))
                .isInstanceOfSatisfying(OwnOverlapNeedsConfirmationException.class,
                        warning -> assertThat(warning.existingBooking()).isEqualTo(first));
        assertThat(activeBookingsOfCourt(tenantA, otherCourt)).isZero();
    }

    @Test
    void createsTheSecondBookingOnceThePlayerAcknowledgesTheOverlap_criterion12() {
        inA.create(court, EIGHT_PM);
        UUID otherCourt = courts.addCourt(ONE_HOUR);

        Booking second = inA.createAcknowledgingOverlap(otherCourt, EIGHT_PM);

        assertThat(second.status()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void anExpiredOrAdjacentBookingOfThePlayerDoesNotTriggerTheWarning_criterion12() {
        inA.create(court, EIGHT_PM);
        UUID otherCourt = courts.addCourt(ONE_HOUR);

        Booking adjacent = inA.create(otherCourt, EIGHT_PM.plus(ONE_HOUR));
        clock.advance(Booking.PENDING_HOLD);
        Booking afterExpiry = inA.create(courts.addCourt(ONE_HOUR), EIGHT_PM);

        assertThat(adjacent.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(afterExpiry.status()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void occupancyListsConfirmedAndHeldSlotsInOrderButNotReleasedOnes() {
        inA.create(court, EIGHT_PM);
        Booking confirmed = inA.create(court, EIGHT_PM.plus(ONE_HOUR));
        inA.confirm.confirm(confirmed.id());
        Booking cancelled = inA.create(court, EIGHT_PM.plus(Duration.ofHours(2)));
        inA.cancel.cancel(cancelled.id());
        clock.advance(Booking.PENDING_HOLD);
        currentPlayer.actAs(UUID.randomUUID());
        Booking stillHeld = inA.create(court, EIGHT_PM.minus(ONE_HOUR));

        List<TimePeriod> occupied = inA.occupancy.occupiedPeriods(court, NOW, NOW.plus(Duration.ofDays(1)));

        assertThat(occupied).containsExactly(stillHeld.period(), confirmed.period());
    }

    @Test
    void occupancyOnlyReturnsSlotsInsideTheQueriedRange() {
        Booking booked = inA.create(court, EIGHT_PM);

        assertThat(inA.occupancy.occupiedPeriods(court, EIGHT_PM.plus(ONE_HOUR), EIGHT_PM.plus(Duration.ofHours(2))))
                .isEmpty();
        assertThat(inA.occupancy.occupiedPeriods(court, EIGHT_PM.plus(Duration.ofMinutes(59)),
                EIGHT_PM.plus(ONE_HOUR))).containsExactly(booked.period());
    }

    @Test
    void occupancyRejectsAnInvertedOrTooLongRangeAndAnUnknownCourt() {
        assertThatThrownBy(() -> inA.occupancy.occupiedPeriods(court, EIGHT_PM, EIGHT_PM))
                .isInstanceOf(InvalidOccupancyRangeException.class);
        assertThatThrownBy(() -> inA.occupancy.occupiedPeriods(court, NOW,
                NOW.plus(GetCourtOccupancy.MAX_RANGE).plusSeconds(1)))
                .isInstanceOf(InvalidOccupancyRangeException.class);
        assertThatThrownBy(() -> inA.occupancy.occupiedPeriods(UUID.randomUUID(), NOW, EIGHT_PM))
                .isInstanceOf(CourtNotFoundException.class);
    }

    @Test
    void everyUseCaseRequiresAnAuthenticatedPlayer() {
        Booking pending = inA.create(court, EIGHT_PM);
        currentPlayer.actAs(null);

        assertThatThrownBy(() -> inA.create(court, EIGHT_PM)).isInstanceOf(PlayerNotAuthenticatedException.class);
        assertThatThrownBy(() -> inA.get.get(pending.id())).isInstanceOf(PlayerNotAuthenticatedException.class);
        assertThatThrownBy(() -> inA.confirm.confirm(pending.id()))
                .isInstanceOf(PlayerNotAuthenticatedException.class);
        assertThatThrownBy(() -> inA.cancel.cancel(pending.id()))
                .isInstanceOf(PlayerNotAuthenticatedException.class);
        assertThatThrownBy(() -> inA.occupancy.occupiedPeriods(court, NOW, EIGHT_PM))
                .isInstanceOf(PlayerNotAuthenticatedException.class);
    }

    @Test
    void anotherTenantNeitherSeesNorChangesTheBookingAndCanBookTheSameSlot_criterion13() {
        Booking inTenantA = inA.create(court, EIGHT_PM);

        assertThatThrownBy(() -> inB.get.get(inTenantA.id())).isInstanceOf(BookingNotFoundException.class);
        assertThatThrownBy(() -> inB.confirm.confirm(inTenantA.id())).isInstanceOf(BookingNotFoundException.class);
        assertThatThrownBy(() -> inB.cancel.cancel(inTenantA.id())).isInstanceOf(BookingNotFoundException.class);
        assertThat(inB.occupancy.occupiedPeriods(court, NOW, NOW.plus(Duration.ofDays(1)))).isEmpty();

        Booking inTenantB = inB.create(court, EIGHT_PM);

        assertThat(inTenantB.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(storedStatus(tenantA, inTenantA.id())).isEqualTo("PENDING");
        assertThat(inA.repository.findById(inTenantB.id())).isEmpty();
        assertThat(activeBookingsOfCourt(tenantA)).isEqualTo(1);
        assertThat(activeBookingsOfCourt(tenantB)).isEqualTo(1);
    }

    /** The use cases of one tenant, sharing the test doubles of this test. */
    private final class UseCases {

        private final PostgresBookingRepository repository;
        private final CreatePendingBooking create;
        private final ConfirmBooking confirm;
        private final CancelOwnPendingBooking cancel;
        private final GetBooking get;
        private final GetCourtOccupancy occupancy;

        private UseCases(DataSource tenant) {
            repository = new PostgresBookingRepository(JdbcClient.create(tenant));
            TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(tenant));
            create = new CreatePendingBooking(repository, courts, currentPlayer, events, transactions, clock);
            confirm = new ConfirmBooking(repository, currentPlayer, events, transactions, clock);
            cancel = new CancelOwnPendingBooking(repository, currentPlayer, events, transactions, clock);
            get = new GetBooking(repository, currentPlayer, clock);
            occupancy = new GetCourtOccupancy(repository, courts, currentPlayer, clock);
        }

        private Booking create(UUID courtId, Instant start) {
            return create.create(new CreatePendingBooking.Command(courtId, start, false));
        }

        private Booking createAcknowledgingOverlap(UUID courtId, Instant start) {
            return create.create(new CreatePendingBooking.Command(courtId, start, true));
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

    private int activeBookingsOfCourt(DataSource tenant) {
        return activeBookingsOfCourt(tenant, court);
    }

    private static int activeBookingsOfCourt(DataSource tenant, UUID courtId) {
        return JdbcClient.create(tenant)
                .sql("SELECT count(*) FROM booking WHERE court_id = :courtId AND status IN ('PENDING', 'CONFIRMED')")
                .param("courtId", courtId)
                .query(Integer.class)
                .single();
    }

    private static String storedStatus(DataSource tenant, UUID bookingId) {
        return JdbcClient.create(tenant)
                .sql("SELECT status FROM booking WHERE id = :id")
                .param("id", bookingId)
                .query(String.class)
                .single();
    }
}
