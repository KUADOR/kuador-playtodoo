package com.kuador.platodo.api.identity.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnPlayerApiEnabled;
import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnStaffApiEnabled;
import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.support.CoversTenantEntity;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Staff accounts are isolated per complex (PLATODO-26 criteria 4, 5, 8 and 19), over HTTP with the real tenant
 * resolution, token filter, PostgreSQL and the staff booking endpoints as protected resources of each complex.
 */
@SpringBootTest(properties = {
        ConditionalOnPlayerApiEnabled.PROPERTY + "=true",
        ConditionalOnStaffApiEnabled.PROPERTY + "=true"})
@Import(TwoComplexesWithBookingApis.class)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
@CoversTenantEntity("staff_user")
class StaffTenantIsolationIT {

    private static final TenantId A = TestTenants.A;
    private static final TenantId B = TestTenants.B;
    private static final String FUTURE_SLOT = "2030-01-01T20:00:00Z";
    private static final String STAFF_BOOKINGS = "/api/v1/staff/bookings";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StubCourtSlotRules courts;

    private String emailOfA;
    private UUID staffOfA;

    @BeforeEach
    void setUp() {
        emailOfA = TestAccounts.uniqueEmail("staff-a");
        staffOfA = TestAccounts.staff(A, emailOfA, "ADMIN");
    }

    @Test
    void staffLogsInOnTheHostOfItsComplexWithATokenBoundToIt() throws Exception {
        String token = logIn(A, emailOfA, TestAccounts.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(Duration.ofHours(1).toSeconds()))
                .andReturn().getResponse().getContentAsString();

        JsonNode claims = JSON.readTree(Base64.getUrlDecoder().decode(accessTokenOf(token).split("\\.")[1]));
        assertThat(claims.get("sub").asString()).isEqualTo(staffOfA.toString());
        assertThat(claims.get("tid").asString()).isEqualTo(A.value());
        assertThat(claims.get("typ").asString()).isEqualTo("STAFF");
    }

    /** Criterion 8: credentials of A on the host of B fail exactly like unknown credentials. */
    @Test
    void credentialsOfAOnTheHostOfBAreGenericallyInvalid() throws Exception {
        String onB = logIn(B, emailOfA, TestAccounts.PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();
        String unknown = logIn(A, TestAccounts.uniqueEmail("nobody"), TestAccounts.PASSWORD)
                .andReturn().getResponse().getContentAsString();
        String wrongPassword = logIn(A, emailOfA, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(onB).isEqualTo(unknown).isEqualTo(wrongPassword);
    }

    /** Criterion 19: a STAFF token issued for A is not accepted on the host of B. */
    @Test
    void staffTokenOfAIsUnauthenticatedOnTheHostOfB() throws Exception {
        String tokenOfA = staffToken(A, emailOfA);

        listBookings(A, tokenOfA).andExpect(status().isOk());
        listBookings(B, tokenOfA)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    /** Criteria 4 and 5: a resource of B requested from A is "not found", like an id that does not exist. */
    @Test
    void bookingOfBRequestedFromAIsNotFound() throws Exception {
        String emailOfB = TestAccounts.uniqueEmail("staff-b");
        TestAccounts.staff(B, emailOfB, "STAFF");
        String bookingOfB = bookingId(createBooking(B, staffToken(B, emailOfB)).andExpect(status().isCreated()));
        String tokenOfA = staffToken(A, emailOfA);

        String fromA = getBooking(A, tokenOfA, bookingOfB)
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();
        String missing = getBooking(A, tokenOfA, UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(fromA).isEqualTo(missing);
        listBookings(A, tokenOfA).andExpect(jsonPath("$[?(@.id == '%s')]".formatted(bookingOfB)).isEmpty());
    }

    /** Criterion 4: the token of A cannot change anything in B; nothing is written in B's schema. */
    @Test
    void staffOfACannotCreateDataInB() throws Exception {
        long bookingsInB = bookingsIn(B);

        createBooking(B, staffToken(A, emailOfA)).andExpect(status().isUnauthorized());

        assertThat(bookingsIn(B)).isEqualTo(bookingsInB);
    }

    @Test
    void theSameEmailInTwoComplexesIsTwoUnrelatedAccounts() throws Exception {
        UUID sameEmailInB = TestAccounts.staff(B, emailOfA, "OWNER");

        String tokenOnB = staffToken(B, emailOfA);

        listBookings(B, tokenOnB).andExpect(status().isOk());
        listBookings(A, tokenOnB).andExpect(status().isUnauthorized());
        assertThat(sameEmailInB).isNotEqualTo(staffOfA);
    }

    @Test
    void staffLoginIsNotServedOnTheGlobalHost() throws Exception {
        mvc.perform(loginRequest(TestTenants.GLOBAL_HOST, "/api/v1/auth/staff/login", emailOfA, TestAccounts.PASSWORD))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void aStaffTokenIsNotAcceptedOnTheGlobalHost() throws Exception {
        mvc.perform(loginRequest(TestTenants.GLOBAL_HOST, "/api/v1/auth/players/login", "x@example.com", "x")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken(A, emailOfA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void aMalformedOrForeignTokenIsUnauthenticated() throws Exception {
        mvc.perform(get(STAFF_BOOKINGS).with(onHost(TestTenants.hostOf(A)))
                        .param("from", "2030-01-01T00:00:00Z").param("to", "2030-01-02T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get(STAFF_BOOKINGS).with(onHost(TestTenants.hostOf(A)))
                        .param("from", "2030-01-01T00:00:00Z").param("to", "2030-01-02T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void aPlatformTokenNeedsAnAccessSessionOnTheHostOfAComplex() throws Exception {
        String email = TestAccounts.uniqueEmail("owner");
        TestAccounts.platformOwner(email);
        String platformLogin = "/api/v1/auth/platform/login";
        String token = accessTokenOf(mvc.perform(
                        loginRequest(TestTenants.GLOBAL_HOST, platformLogin, email, TestAccounts.PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        listBookings(A, token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_SESSION_INVALID"));
    }

    @Test
    void aMalformedLoginIsAnInvalidRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/staff/login").with(onHost(TestTenants.hostOf(A)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\"}".formatted(emailOfA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private String staffToken(TenantId tenant, String email) throws Exception {
        return accessTokenOf(logIn(tenant, email, TestAccounts.PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private ResultActions logIn(TenantId tenant, String email, String password) throws Exception {
        return mvc.perform(loginRequest(TestTenants.hostOf(tenant), "/api/v1/auth/staff/login", email, password));
    }

    private static MockHttpServletRequestBuilder loginRequest(String host, String path, String email,
            String password) {
        return post(path).with(onHost(host))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }

    private ResultActions listBookings(TenantId tenant, String token) throws Exception {
        return mvc.perform(get(STAFF_BOOKINGS).with(onHost(TestTenants.hostOf(tenant)))
                .param("from", "2030-01-01T00:00:00Z")
                .param("to", "2030-01-02T00:00:00Z")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions getBooking(TenantId tenant, String token, String bookingId) throws Exception {
        return mvc.perform(get(STAFF_BOOKINGS + "/" + bookingId).with(onHost(TestTenants.hostOf(tenant)))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions createBooking(TenantId tenant, String token) throws Exception {
        return mvc.perform(post(STAFF_BOOKINGS).with(onHost(TestTenants.hostOf(tenant)))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"courtId": "%s", "start": "%s", "holderName": "Alex Example"}
                        """.formatted(courts.addCourt(Duration.ofMinutes(60)), FUTURE_SLOT)));
    }

    private static long bookingsIn(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT count(*) FROM \"%s\".booking".formatted(TestTenants.schemaOf(tenant)))
                .query(Long.class)
                .single();
    }

    private static String bookingId(ResultActions created) throws Exception {
        return JSON.readTree(created.andReturn().getResponse().getContentAsString())
                .get("id").asString();
    }

    private static String accessTokenOf(String loginResponse) {
        return JSON.readTree(loginResponse).get("accessToken").asString();
    }
}
