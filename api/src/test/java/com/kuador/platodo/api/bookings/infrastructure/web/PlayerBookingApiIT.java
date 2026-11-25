package com.kuador.platodo.api.bookings.infrastructure.web;

import static com.kuador.platodo.api.support.TestTenants.FLYWAY_INITIALIZER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.event.BookingConfirmed;
import com.kuador.platodo.api.bookings.domain.event.BookingCreated;
import com.kuador.platodo.api.bookings.domain.event.BookingEvent;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.bookings.support.StubCurrentPlayer;
import com.kuador.platodo.api.support.FixedTenantScopeFilter;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

/**
 * Player booking endpoints over HTTP, as defined in contracts/bookings.openapi.yaml, with the real wiring,
 * PostgreSQL and test doubles for the ports that other modules will provide (courts, current player).
 */
@SpringBootTest(properties = ConditionalOnPlayerApiEnabled.PROPERTY + "=true")
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class PlayerBookingApiIT {

    private static final TenantId TENANT = new TenantId("0c000000-0000-4000-8000-000000000001");

    private static final Instant NOW = Instant.parse("2026-12-04T18:00:00Z");
    private static final String EIGHT_PM = "2026-12-04T20:00:00Z";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @TestConfiguration(proxyBeanMethods = false)
    static class Doubles {

        /** Stand-in for the subdomain resolution of PLATODO-50: every request runs in this class's tenant. */
        @Bean
        @DependsOn(FLYWAY_INITIALIZER)
        FixedTenantScopeFilter tenantScope(TenantMigrations migrations) {
            return new FixedTenantScopeFilter(TestTenants.provision(migrations, TENANT));
        }

        @Bean
        StubCourtSlotRules courtSlotRules() {
            return new StubCourtSlotRules();
        }

        @Bean
        StubCurrentPlayer currentPlayer() {
            return new StubCurrentPlayer();
        }

        @Bean
        MutableClock clock() {
            return new MutableClock(NOW);
        }

        @Bean
        CommittedBookingEvents committedBookingEvents() {
            return new CommittedBookingEvents();
        }
    }

    /** Listens as future consumers must: after the transaction commits. */
    static class CommittedBookingEvents {

        private final List<BookingEvent> received = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        void on(BookingEvent event) {
            received.add(event);
        }

        List<BookingEvent> about(UUID bookingId) {
            return received.stream().filter(event -> event.bookingId().equals(bookingId)).toList();
        }

        List<BookingEvent> all() {
            return List.copyOf(received);
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StubCourtSlotRules courts;
    @Autowired
    private StubCurrentPlayer currentPlayer;
    @Autowired
    private MutableClock clock;
    @Autowired
    private CommittedBookingEvents events;

    private UUID court;

    @BeforeEach
    void setUp() {
        clock.setTo(NOW);
        court = courts.addCourt(Duration.ofMinutes(90));
        currentPlayer.actAs(UUID.randomUUID());
    }

    @Test
    void createsAPendingBookingWithItsLocationAndHold() throws Exception {
        createBooking(court, EIGHT_PM, false)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/bookings/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.courtId").value(court.toString()))
                .andExpect(jsonPath("$.start").value(EIGHT_PM))
                .andExpect(jsonPath("$.end").value("2026-12-04T21:30:00Z"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-04T18:05:00Z"))
                .andExpect(jsonPath("$.createdAt").value("2026-12-04T18:00:00Z"))
                .andExpect(jsonPath("$.confirmedAt").isEmpty())
                .andExpect(jsonPath("$.cancelledAt").isEmpty());
    }

    @Test
    void acceptsAStartWithAnOffsetAndAnswersInUtc() throws Exception {
        createBooking(court, "2026-12-04T15:00:00-05:00", false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.start").value(EIGHT_PM));
    }

    @Test
    void aTakenSlotIsAConflict() throws Exception {
        createBooking(court, EIGHT_PM, false).andExpect(status().isCreated());
        currentPlayer.actAs(UUID.randomUUID());

        expectProblem(createBooking(court, "2026-12-04T21:00:00Z", false), 409, "SLOT_UNAVAILABLE",
                "https://playtodoo.com/problems/slot-unavailable");
        assertThat(events.all()).noneMatch(event -> event instanceof BookingCreated created
                && created.start().equals(Instant.parse("2026-12-04T21:00:00Z")));
    }

    @Test
    void anOverlappingBookingOfTheSamePlayerNeedsConfirmation() throws Exception {
        String first = bookingId(createBooking(court, EIGHT_PM, false));
        UUID otherCourt = courts.addCourt(Duration.ofHours(1));

        expectProblem(createBooking(otherCourt, EIGHT_PM, false), 409, "OWN_OVERLAP_NEEDS_CONFIRMATION",
                "https://playtodoo.com/problems/own-overlap-needs-confirmation")
                .andExpect(jsonPath("$.existingBooking.id").value(first))
                .andExpect(jsonPath("$.existingBooking.status").value("PENDING"));
        createBooking(otherCourt, EIGHT_PM, true).andExpect(status().isCreated());
    }

    @Test
    void anInvalidStartIsUnprocessable() throws Exception {
        expectProblem(createBooking(court, "2026-12-04T20:10:00Z", false), 422, "INVALID_SLOT",
                "https://playtodoo.com/problems/invalid-slot");
    }

    @Test
    void anUnknownCourtIsNotFound() throws Exception {
        expectProblem(createBooking(UUID.randomUUID(), EIGHT_PM, false), 404, "COURT_NOT_FOUND",
                "https://playtodoo.com/problems/court-not-found");
    }

    @Test
    void aMalformedBodyIsAnInvalidRequest() throws Exception {
        expectProblem(mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"start\":\"" + EIGHT_PM + "\"}")), 400, "INVALID_REQUEST",
                "https://playtodoo.com/problems/invalid-request");
        expectProblem(mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"courtId\":\"" + court + "\",\"start\":\"tonight\"}")), 400, "INVALID_REQUEST",
                "https://playtodoo.com/problems/invalid-request");
    }

    @Test
    void confirmsWithinTheHoldAndPublishesAfterCommit() throws Exception {
        String id = bookingId(createBooking(court, EIGHT_PM, false));
        clock.advance(Duration.ofMinutes(4));

        mvc.perform(post("/api/v1/bookings/{id}/confirm", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").value("2026-12-04T18:04:00Z"));

        assertThat(events.about(UUID.fromString(id)))
                .hasExactlyElementsOfTypes(BookingCreated.class, BookingConfirmed.class);
    }

    @Test
    void confirmingAfterTheHoldIsExpiredAndReadsAsExpired() throws Exception {
        String id = bookingId(createBooking(court, EIGHT_PM, false));
        clock.advance(Booking.PENDING_HOLD);

        expectProblem(mvc.perform(post("/api/v1/bookings/{id}/confirm", id)), 409, "BOOKING_EXPIRED",
                "https://playtodoo.com/problems/booking-expired");
        mvc.perform(get("/api/v1/bookings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void cancelsOwnPendingBookingButNotAConfirmedOne() throws Exception {
        String pending = bookingId(createBooking(court, EIGHT_PM, false));
        String confirmed = bookingId(createBooking(court, "2026-12-04T22:00:00Z", false));
        mvc.perform(post("/api/v1/bookings/{id}/confirm", confirmed)).andExpect(status().isOk());

        mvc.perform(post("/api/v1/bookings/{id}/cancel", pending))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-12-04T18:00:00Z"));
        expectProblem(mvc.perform(post("/api/v1/bookings/{id}/cancel", confirmed)), 409, "BOOKING_NOT_PENDING",
                "https://playtodoo.com/problems/booking-not-pending");
    }

    @Test
    void anotherPlayersBookingIsNotFound() throws Exception {
        String id = bookingId(createBooking(court, EIGHT_PM, false));
        currentPlayer.actAs(UUID.randomUUID());

        expectProblem(mvc.perform(get("/api/v1/bookings/{id}", id)), 404, "BOOKING_NOT_FOUND",
                "https://playtodoo.com/problems/booking-not-found");
    }

    @Test
    void listsTheOccupiedRangesOfACourt() throws Exception {
        createBooking(court, EIGHT_PM, false).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/courts/{courtId}/occupancy", court)
                        .param("from", "2026-12-04T00:00:00Z")
                        .param("to", "2026-12-05T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].start").value(EIGHT_PM))
                .andExpect(jsonPath("$[0].end").value("2026-12-04T21:30:00Z"))
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    void anInvertedOrMissingOccupancyRangeIsAnInvalidRequest() throws Exception {
        expectProblem(mvc.perform(get("/api/v1/courts/{courtId}/occupancy", court)
                        .param("from", "2026-12-05T00:00:00Z")
                        .param("to", "2026-12-04T00:00:00Z")), 400, "INVALID_REQUEST",
                "https://playtodoo.com/problems/invalid-request");
        expectProblem(mvc.perform(get("/api/v1/courts/{courtId}/occupancy", court)
                        .param("from", "2026-12-05T00:00:00Z")), 400, "INVALID_REQUEST",
                "https://playtodoo.com/problems/invalid-request");
    }

    @Test
    void aRequestWithoutPlayerIsUnauthenticated() throws Exception {
        currentPlayer.actAs(null);

        expectProblem(createBooking(court, EIGHT_PM, false), 401, "UNAUTHENTICATED",
                "https://playtodoo.com/problems/unauthenticated");
    }

    private ResultActions createBooking(UUID courtId, String start, boolean acknowledgeOwnOverlap)
            throws Exception {
        return mvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"courtId": "%s", "start": "%s", "acknowledgeOwnOverlap": %s}
                        """.formatted(courtId, start, acknowledgeOwnOverlap)));
    }

    private static String bookingId(ResultActions created) throws Exception {
        String location = created.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private static ResultActions expectProblem(ResultActions response, int status, String code, String type)
            throws Exception {
        return response
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }
}
