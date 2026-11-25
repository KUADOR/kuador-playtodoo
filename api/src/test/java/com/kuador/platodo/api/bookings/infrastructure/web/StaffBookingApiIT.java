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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kuador.platodo.api.bookings.domain.event.BookingCancelled;
import com.kuador.platodo.api.bookings.domain.event.BookingCreated;
import com.kuador.platodo.api.bookings.domain.event.BookingEvent;
import com.kuador.platodo.api.bookings.domain.event.SlotBlocked;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.bookings.support.StubCurrentPlayer;
import com.kuador.platodo.api.bookings.support.StubCurrentStaff;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TenantHostRequests;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

/**
 * Staff booking endpoints over HTTP, as defined in contracts/bookings.openapi.yaml, with the real wiring of
 * both booking APIs, PostgreSQL and test doubles for the ports that other modules will provide.
 * Numbers in test names refer to the acceptance criteria of PLATODO-25.
 */
@SpringBootTest(properties = {
        ConditionalOnStaffApiEnabled.PROPERTY + "=true",
        ConditionalOnPlayerApiEnabled.PROPERTY + "=true"})
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class StaffBookingApiIT {

    private static final TenantId TENANT = new TenantId("0c000000-0000-4000-8000-000000000003");

    private static final Instant NOW = Instant.parse("2026-12-04T18:00:00Z");
    private static final String EIGHT_PM = "2026-12-04T20:00:00Z";
    private static final String NINE_THIRTY_PM = "2026-12-04T21:30:00Z";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @TestConfiguration(proxyBeanMethods = false)
    static class Doubles {

        /** Every request goes to the host of this class's tenant and is resolved by Tenancy. */
        @Bean
        @DependsOn(FLYWAY_INITIALIZER)
        TenantHostRequests tenantHost(TenantMigrations migrations) {
            return new TenantHostRequests(TestTenants.provision(migrations, TENANT));
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
        StubCurrentStaff currentStaff() {
            return new StubCurrentStaff();
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
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StubCourtSlotRules courts;
    @Autowired
    private StubCurrentPlayer currentPlayer;
    @Autowired
    private StubCurrentStaff currentStaff;
    @Autowired
    private MutableClock clock;
    @Autowired
    private CommittedBookingEvents events;

    private UUID court;

    @BeforeEach
    void setUp() {
        clock.setTo(NOW);
        court = courts.addCourt(Duration.ofMinutes(90));
        currentStaff.actAs(UUID.randomUUID());
        currentPlayer.actAs(UUID.randomUUID());
    }

    @Test
    void createsAConfirmedManualBookingWithItsLocation() throws Exception {
        String id = entryId(bookManually(court, EIGHT_PM, "Alex Example")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/staff/bookings/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.courtId").value(court.toString()))
                .andExpect(jsonPath("$.kind").value("RESERVATION"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.start").value(EIGHT_PM))
                .andExpect(jsonPath("$.end").value(NINE_THIRTY_PM))
                .andExpect(jsonPath("$.holderName").value("Alex Example"))
                .andExpect(jsonPath("$.blockReason").isEmpty())
                .andExpect(jsonPath("$.holderPhone").doesNotExist()));

        assertThat(events.about(UUID.fromString(id))).hasExactlyElementsOfTypes(BookingCreated.class);
    }

    @Test
    void aManualBookingOnATakenSlotIsUnavailable_criterion8() throws Exception {
        createPlayerBooking(court, EIGHT_PM).andExpect(status().isCreated());

        expectProblem(bookManually(court, "2026-12-04T21:00:00Z", "Alex Example"), 409, "SLOT_UNAVAILABLE",
                "https://playtodoo.com/problems/slot-unavailable");
    }

    @Test
    void aManualBookingWithAnInvalidStartOrUnknownCourtIsRejected() throws Exception {
        expectProblem(bookManually(court, "2026-12-04T20:10:00Z", "Alex Example"), 422, "INVALID_SLOT",
                "https://playtodoo.com/problems/invalid-slot");
        expectProblem(bookManually(UUID.randomUUID(), EIGHT_PM, "Alex Example"), 404, "COURT_NOT_FOUND",
                "https://playtodoo.com/problems/court-not-found");
    }

    @Test
    void aManualBookingWithoutHolderOrWithTooLongFieldsIsAnInvalidRequest() throws Exception {
        expectInvalidRequest(post("/api/v1/staff/bookings"), """
                {"courtId": "%s", "start": "%s"}
                """.formatted(court, EIGHT_PM));
        expectInvalidRequest(post("/api/v1/staff/bookings"), """
                {"courtId": "%s", "start": "%s", "holderName": "   "}
                """.formatted(court, EIGHT_PM));
        expectInvalidRequest(post("/api/v1/staff/bookings"), """
                {"courtId": "%s", "start": "%s", "holderName": "%s"}
                """.formatted(court, EIGHT_PM, "x".repeat(121)));
        expectInvalidRequest(post("/api/v1/staff/bookings"), """
                {"courtId": "%s", "start": "%s", "holderName": "Alex", "holderPhone": "%s"}
                """.formatted(court, EIGHT_PM, "0".repeat(33)));
        expectInvalidRequest(post("/api/v1/staff/blocks"), """
                {"courtId": "%s", "start": "%s", "reason": "%s"}
                """.formatted(court, EIGHT_PM, "x".repeat(201)));
    }

    @Test
    void blocksAFreeSlotAndPlayersSeeItOccupied() throws Exception {
        String id = entryId(block(court, EIGHT_PM, "Maintenance")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/staff/bookings/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.kind").value("BLOCK"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.holderName").isEmpty())
                .andExpect(jsonPath("$.blockReason").value("Maintenance")));

        assertThat(events.about(UUID.fromString(id))).hasExactlyElementsOfTypes(SlotBlocked.class);
        expectProblem(createPlayerBooking(court, EIGHT_PM), 409, "SLOT_UNAVAILABLE",
                "https://playtodoo.com/problems/slot-unavailable");
    }

    @Test
    void aBlockOnABookedSlotNamesTheBookingAndKeepsIt_criterion9() throws Exception {
        String booking = entryId(bookManually(court, EIGHT_PM, "Alex Example"));

        expectProblem(block(court, "2026-12-04T21:00:00Z", null), 409, "SLOT_HAS_BOOKING",
                "https://playtodoo.com/problems/slot-has-booking")
                .andExpect(jsonPath("$.conflictingBookingId").value(booking));
        mvc.perform(get("/api/v1/staff/bookings/{id}", booking))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void aBlockOnABlockedSlotIsUnavailable() throws Exception {
        block(court, EIGHT_PM, null).andExpect(status().isCreated());

        expectProblem(block(court, EIGHT_PM, null), 409, "SLOT_UNAVAILABLE",
                "https://playtodoo.com/problems/slot-unavailable");
    }

    @Test
    void aBlockIsAcceptedAfterCancellingTheBooking_criterion10() throws Exception {
        String booking = entryId(createPlayerBooking(court, EIGHT_PM));
        block(court, EIGHT_PM, null).andExpect(status().is(409));

        mvc.perform(post("/api/v1/staff/bookings/{id}/cancel", booking))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        block(court, EIGHT_PM, null).andExpect(status().isCreated());

        assertThat(events.about(UUID.fromString(booking)))
                .hasExactlyElementsOfTypes(BookingCreated.class, BookingCancelled.class);
    }

    @Test
    void removingABlockFreesTheSlotAndCancellingTwiceIsASafeRetry() throws Exception {
        String blocked = entryId(block(court, EIGHT_PM, null));

        mvc.perform(post("/api/v1/staff/bookings/{id}/cancel", blocked))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("BLOCK"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/v1/staff/bookings/{id}/cancel", blocked))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        createPlayerBooking(court, EIGHT_PM).andExpect(status().isCreated());
        assertThat(events.about(UUID.fromString(blocked)))
                .hasExactlyElementsOfTypes(SlotBlocked.class, BookingCancelled.class);
    }

    @Test
    void anUnknownBookingIsNotFound() throws Exception {
        expectProblem(mvc.perform(post("/api/v1/staff/bookings/{id}/cancel", UUID.randomUUID())), 404,
                "BOOKING_NOT_FOUND", "https://playtodoo.com/problems/booking-not-found");
        expectProblem(mvc.perform(get("/api/v1/staff/bookings/{id}", UUID.randomUUID())), 404,
                "BOOKING_NOT_FOUND", "https://playtodoo.com/problems/booking-not-found");
    }

    @Test
    void theCalendarHidesTheHolderOfPendingBookings() throws Exception {
        String pending = entryId(createPlayerBooking(court, EIGHT_PM));
        String manual = entryId(bookManually(court, NINE_THIRTY_PM, "Alex Example"));
        String blocked = entryId(block(court, "2026-12-04T23:00:00Z", "Maintenance"));
        bookManually(courts.addCourt(Duration.ofHours(1)), EIGHT_PM, "Other Court").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/staff/bookings")
                        .param("from", "2026-12-04T00:00:00Z")
                        .param("to", "2026-12-05T03:00:00Z")
                        .param("courtId", court.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(pending))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].holderName").isEmpty())
                .andExpect(jsonPath("$[1].id").value(manual))
                .andExpect(jsonPath("$[1].holderName").value("Alex Example"))
                .andExpect(jsonPath("$[2].id").value(blocked))
                .andExpect(jsonPath("$[2].kind").value("BLOCK"))
                .andExpect(jsonPath("$[2].blockReason").value("Maintenance"));
    }

    @Test
    void anInvertedOrMissingRangeIsAnInvalidRequest() throws Exception {
        expectProblem(mvc.perform(get("/api/v1/staff/bookings")
                        .param("from", "2026-12-05T00:00:00Z")
                        .param("to", "2026-12-04T00:00:00Z")), 400, "INVALID_REQUEST",
                "https://playtodoo.com/problems/invalid-request");
        expectProblem(mvc.perform(get("/api/v1/staff/bookings")
                        .param("from", "2026-12-05T00:00:00Z")), 400, "INVALID_REQUEST",
                "https://playtodoo.com/problems/invalid-request");
    }

    @Test
    void aCallerWithoutStaffRoleInTheTenantIsForbiddenAndChangesNothing() throws Exception {
        String booking = entryId(bookManually(court, EIGHT_PM, "Alex Example"));
        currentStaff.actAsOutsider();

        expectProblem(mvc.perform(post("/api/v1/staff/bookings/{id}/cancel", booking)), 403, "FORBIDDEN",
                "https://playtodoo.com/problems/forbidden");
        expectProblem(block(court, "2026-12-05T01:00:00Z", null), 403, "FORBIDDEN",
                "https://playtodoo.com/problems/forbidden");
        expectProblem(mvc.perform(get("/api/v1/staff/bookings")
                        .param("from", "2026-12-04T00:00:00Z")
                        .param("to", "2026-12-05T00:00:00Z")), 403, "FORBIDDEN",
                "https://playtodoo.com/problems/forbidden");

        currentStaff.actAs(UUID.randomUUID());
        mvc.perform(get("/api/v1/staff/bookings/{id}", booking)).andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void aRequestWithoutStaffIsUnauthenticated() throws Exception {
        currentStaff.actAs(null);

        expectProblem(bookManually(court, EIGHT_PM, "Alex Example"), 401, "UNAUTHENTICATED",
                "https://playtodoo.com/problems/unauthenticated");
    }

    private ResultActions bookManually(UUID courtId, String start, String holderName) throws Exception {
        return mvc.perform(post("/api/v1/staff/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"courtId": "%s", "start": "%s", "holderName": "%s", "holderPhone": "+000 0000 0000"}
                        """.formatted(courtId, start, holderName)));
    }

    private ResultActions block(UUID courtId, String start, String reason) throws Exception {
        String reasonJson = reason == null ? "null" : "\"" + reason + "\"";
        return mvc.perform(post("/api/v1/staff/blocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"courtId": "%s", "start": "%s", "reason": %s}
                        """.formatted(courtId, start, reasonJson)));
    }

    private ResultActions createPlayerBooking(UUID courtId, String start) throws Exception {
        return mvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"courtId": "%s", "start": "%s", "acknowledgeOwnOverlap": true}
                        """.formatted(courtId, start)));
    }

    private void expectInvalidRequest(MockHttpServletRequestBuilder request, String body) throws Exception {
        expectProblem(mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body)), 400,
                "INVALID_REQUEST", "https://playtodoo.com/problems/invalid-request");
    }

    private static String entryId(ResultActions created) throws Exception {
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
