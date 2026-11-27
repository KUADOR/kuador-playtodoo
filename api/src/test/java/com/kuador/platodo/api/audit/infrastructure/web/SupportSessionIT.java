package com.kuador.platodo.api.audit.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static com.kuador.platodo.api.support.TestTenants.FLYWAY_INITIALIZER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnStaffApiEnabled;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Support sessions with a reason, edition and audit (ADR-017, ADR-020; PLATODO-27 CA11 to CA14 and CA17), over HTTP
 * with the real tenant resolution, token filter, support access guard, staff booking operations and PostgreSQL. A
 * probe endpoint of the tenant host reports the schema its connection uses and counts its own runs, so a denial is
 * shown to touch no tenant schema.
 */
@SpringBootTest(properties = ConditionalOnStaffApiEnabled.PROPERTY + "=true")
@Import(SupportSessionIT.ProbeOnTenantHost.class)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class SupportSessionIT {

    private static final TenantId A = TestTenants.A;
    private static final TenantId B = TestTenants.B;
    private static final TenantId SUSPENDED = new TenantId("0e880000-0000-4000-8000-000000000001");
    private static final String PROBE = "/api/v1/test-probe/schema";
    private static final String STAFF_BOOKINGS = "/api/v1/staff/bookings";
    private static final String PLATFORM_LOGIN = "/api/v1/auth/platform/login";
    private static final String PLAYER_LOGIN = "/api/v1/auth/players/login";
    private static final String STAFF_LOGIN = "/api/v1/auth/staff/login";
    private static final String SESSION_HEADER = "X-Support-Session";
    private static final String BACKOFFICE_ORIGIN = "http://admin.localhost:8000";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String REASON = "Complaint about a duplicated booking, ticket 1234";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ProbeOnTenantHost probe;

    private UUID superAdminId;
    private String superAdminToken;
    private UUID operatorId;
    private String operatorToken;

    @BeforeEach
    void setUp() throws Exception {
        String superAdminEmail = TestAccounts.uniqueEmail("super-admin");
        superAdminId = TestAccounts.platformOwner(superAdminEmail);
        superAdminToken = logIn(PLATFORM_LOGIN, superAdminEmail);
        String operatorEmail = TestAccounts.uniqueEmail("operator");
        operatorId = TestAccounts.operator(operatorEmail);
        operatorToken = logIn(PLATFORM_LOGIN, operatorEmail);
        setStatus(SUSPENDED, "SUSPENDED");
    }

    @AfterEach
    void disableOperatorAccess() {
        tenantDataAccessForOperators(false);
    }

    /** CA11: with a reason the SUPER_ADMIN reaches complex A, audited first and only inside the schema of A. */
    @Test
    void withAReasonTheSuperAdminReachesTheComplexAuditedAndOnlyInItsSchema() throws Exception {
        String opened = openSession(A, superAdminToken, REASON)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantHost").value(TestTenants.hostOf(A)))
                .andExpect(jsonPath("$.tenantStatus").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String sessionId = JSON.readTree(opened).get("sessionId").asString();
        Instant expiresAt = Instant.parse(JSON.readTree(opened).get("expiresAt").asString());
        assertThat(Duration.between(Instant.now(), expiresAt))
                .isBetween(Duration.ofMinutes(29), Duration.ofMinutes(30));

        mvc.perform(get(PROBE).with(onHost(TestTenants.hostOf(A)))
                        .param("email", "someone@example.com")
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken))
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant").value(A.value()))
                .andExpect(jsonPath("$.schema").value(TestTenants.schemaOf(A)));

        JsonNode request = auditEntriesOf(A).get(0);
        assertThat(request.get("result").asString()).isEqualTo("ALLOWED");
        assertThat(request.get("sessionId").asString()).isEqualTo(sessionId);
        assertThat(request.get("actor").get("id").asString()).isEqualTo(superAdminId.toString());
        assertThat(request.get("actor").get("displayName").asString()).isEqualTo(TestAccounts.PLATFORM_USER_NAME);
        assertThat(request.get("reason").asString()).isEqualTo(REASON);
        assertThat(request.get("reasonRedacted").asBoolean()).isFalse();
        assertThat(request.get("httpMethod").asString()).isEqualTo("GET");
        assertThat(request.get("pathTemplate").asString()).isEqualTo(PROBE);
        assertThat(request.get("operationId").isNull()).isTrue();
        assertThat(request.get("denialCode").isNull()).isTrue();
        JsonNode opening = auditEntriesOf(A).get(1);
        assertThat(opening.get("operationId").asString()).isEqualTo("openSupportSession");
        assertThat(opening.get("sessionId").asString()).isEqualTo(sessionId);
    }

    /** CA11 and CA17: support reuses the staff operations, audited with their contract operationId. */
    @Test
    void supportUsesTheStaffOperationsAndTheAuditNamesTheOperation() throws Exception {
        String sessionId = sessionIdOf(openSession(A, superAdminToken, REASON));

        onTenant(listStaffBookings(), A, superAdminToken, sessionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        JsonNode entry = auditEntriesOf(A).get(0);
        assertThat(entry.get("operationId").asString()).isEqualTo("listStaffBookings");
        assertThat(entry.get("pathTemplate").asString()).isEqualTo(STAFF_BOOKINGS);
    }

    /** CA11: without a reason no session is opened and no data is reached; the refusal is audited. */
    @Test
    void withoutAReasonNoSessionIsOpenedAndThereIsNoData() throws Exception {
        long sessionsBefore = sessionsOf(superAdminId);
        int runsBefore = probe.runs();

        openSession(A, superAdminToken, "   ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        mvc.perform(post(sessionsPath(A)).with(onHost(TestTenants.GLOBAL_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        openSession(A, superAdminToken, "x".repeat(501))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        probeOn(A, superAdminToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));

        assertThat(sessionsOf(superAdminId)).isEqualTo(sessionsBefore);
        assertThat(probe.runs()).isEqualTo(runsBefore);
        JsonNode refusal = auditEntriesOf(A).get(1);
        assertThat(refusal.get("result").asString()).isEqualTo("DENIED");
        assertThat(refusal.get("denialCode").asString()).isEqualTo("REASON_REQUIRED");
        assertThat(refusal.get("operationId").asString()).isEqualTo("openSupportSession");
        assertThat(refusal.get("sessionId").isNull()).isTrue();
    }

    /** Isolation: a session of A used on the host of B is denied without touching the schema of B, audited on B. */
    @Test
    void theSessionOfAUsedOnTheHostOfBIsDeniedWithoutTouchingB() throws Exception {
        String sessionId = sessionIdOf(openSession(A, superAdminToken, REASON));
        int runsBefore = probe.runs();

        probeOn(B, superAdminToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));
        onTenant(listStaffBookings(), B, superAdminToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));

        assertThat(probe.runs()).isEqualTo(runsBefore);
        JsonNode denial = auditEntriesOf(B).get(0);
        assertThat(denial.get("result").asString()).isEqualTo("DENIED");
        assertThat(denial.get("denialCode").asString()).isEqualTo("SUPPORT_SESSION_INVALID");
        assertThat(denial.get("actor").get("id").asString()).isEqualTo(superAdminId.toString());
        assertThat(denial.get("sessionId").isNull()).isTrue();
        assertThat(denial.get("reason").isNull()).isTrue();
        assertThat(denial.get("operationId").asString()).isEqualTo("listStaffBookings");
        assertThat(auditRowsOutsideTenant(A, sessionId)).isZero();
    }

    /** CA12 and ADR-017: the operator needs TENANT_DATA_ACCESS, and losing it stops the next request. */
    @Test
    void anOperatorNeedsTheCapabilityAndLosesAccessOnTheNextRequestWhenItIsDisabled() throws Exception {
        openSession(A, operatorToken, REASON)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CAPABILITY_REVOKED"));

        tenantDataAccessForOperators(true);
        String sessionId = sessionIdOf(openSession(A, operatorToken, REASON));
        probeOn(A, operatorToken, sessionId).andExpect(status().isOk());

        tenantDataAccessForOperators(false);
        probeOn(A, operatorToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));
    }

    /** CA13: on a suspended complex only the SUPER_ADMIN works, reading and editing; the operator is refused. */
    @Test
    void onASuspendedComplexOnlyTheSuperAdminWorks() throws Exception {
        tenantDataAccessForOperators(true);
        openSession(SUSPENDED, operatorToken, REASON)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_NOT_ACCESSIBLE"));

        String sessionId = sessionIdOf(openSession(SUSPENDED, superAdminToken, REASON)
                .andExpect(jsonPath("$.tenantStatus").value("SUSPENDED")));
        probeOn(SUSPENDED, superAdminToken, sessionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schema").value(TestTenants.schemaOf(SUSPENDED)));
        onTenant(listStaffBookings(), SUSPENDED, superAdminToken, sessionId).andExpect(status().isOk());

        setStatus(SUSPENDED, "ARCHIVED");
        probeOn(SUSPENDED, superAdminToken, sessionId).andExpect(status().isOk());
    }

    /** An operator session on an active complex stops working once the complex is suspended. */
    @Test
    void anOperatorSessionStopsWorkingWhenTheComplexIsSuspended() throws Exception {
        tenantDataAccessForOperators(true);
        setStatus(SUSPENDED, "ACTIVE");
        String sessionId = sessionIdOf(openSession(SUSPENDED, operatorToken, REASON));
        probeOn(SUSPENDED, operatorToken, sessionId).andExpect(status().isOk());

        setStatus(SUSPENDED, "SUSPENDED");
        int runsBefore = probe.runs();

        probeOn(SUSPENDED, operatorToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));
        assertThat(probe.runs()).isEqualTo(runsBefore);
    }

    /** ADR-020: announcing a support session never lets anyone else into a suspended complex. */
    @Test
    void theSupportHeaderDoesNotOpenASuspendedComplexToAnyoneElse() throws Exception {
        String sessionId = sessionIdOf(openSession(SUSPENDED, superAdminToken, REASON));
        String staffEmail = TestAccounts.uniqueEmail("staff");
        TestAccounts.staff(SUSPENDED, staffEmail, "OWNER");
        String playerEmail = TestAccounts.uniqueEmail("player");
        TestAccounts.player(playerEmail);
        String playerToken = logIn(PLAYER_LOGIN, playerEmail);
        int runsBefore = probe.runs();

        probeOn(SUSPENDED, null, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_UNAVAILABLE"));
        probeOn(SUSPENDED, playerToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_UNAVAILABLE"));
        mvc.perform(post(STAFF_LOGIN).with(onHost(TestTenants.hostOf(SUSPENDED)))
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(staffEmail, TestAccounts.PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_UNAVAILABLE"));
        mvc.perform(get("/api/v1/tenant").with(onHost(TestTenants.hostOf(SUSPENDED)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken))
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_UNAVAILABLE"));

        assertThat(probe.runs()).isEqualTo(runsBefore);
    }

    @Test
    void aPlatformTokenWithoutAValidSessionIsRejectedOnTheHostOfAComplex() throws Exception {
        int runsBefore = probe.runs();

        probeOn(A, superAdminToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));
        probeOn(A, superAdminToken, "not-a-session-id")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));
        String othersSession = sessionIdOf(openSession(A, superAdminToken, REASON));
        tenantDataAccessForOperators(true);
        probeOn(A, operatorToken, othersSession)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));

        assertThat(probe.runs()).isEqualTo(runsBefore);
    }

    @Test
    void aClosedSessionNoLongerGrantsAccessAndOnlyWhoOpenedItClosesIt() throws Exception {
        tenantDataAccessForOperators(true);
        String sessionId = sessionIdOf(openSession(A, operatorToken, REASON));
        probeOn(A, operatorToken, sessionId).andExpect(status().isOk());

        closeSession(sessionId, superAdminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_NOT_FOUND"));
        closeSession(UUID.randomUUID().toString(), operatorToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_NOT_FOUND"));
        closeSession(sessionId, operatorToken).andExpect(status().isNoContent());
        closeSession(sessionId, operatorToken).andExpect(status().isNoContent());

        probeOn(A, operatorToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));
        JsonNode closing = auditEntriesOf(A).get(1);
        assertThat(closing.get("operationId").asString()).isEqualTo("closeSupportSession");
        assertThat(closing.get("sessionId").asString()).isEqualTo(sessionId);
    }

    /** CA20 of PLATODO-26: when the audit cannot be written, a valid session is denied and the handler never runs. */
    @Test
    void whenTheAuditCannotBeWrittenTheAccessIsDenied() throws Exception {
        String sessionId = sessionIdOf(openSession(A, superAdminToken, REASON));
        int runsBefore = probe.runs();
        failAllowedAuditInserts();
        try {
            probeOn(A, superAdminToken, sessionId)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("SUPPORT_SESSION_INVALID"));
        } finally {
            restoreAuditInserts();
        }

        assertThat(probe.runs()).isEqualTo(runsBefore);
        probeOn(A, superAdminToken, sessionId).andExpect(status().isOk());
    }

    /** CA14: only the SUPER_ADMIN reads the audit, newest first and only for that complex. */
    @Test
    void onlyTheSuperAdminReadsTheAuditPagedNewestFirst() throws Exception {
        String sessionId = sessionIdOf(openSession(A, superAdminToken, REASON));
        probeOn(A, superAdminToken, sessionId).andExpect(status().isOk());
        probeOn(A, superAdminToken, null).andExpect(status().isForbidden());

        String page = mvc.perform(get(auditPath(A)).with(onHost(TestTenants.GLOBAL_HOST))
                        .param("page", "0").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].result").value("DENIED"))
                .andExpect(jsonPath("$.items[1].result").value("ALLOWED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(JSON.readTree(page).get("totalItems").asLong()).isGreaterThanOrEqualTo(3);

        tenantDataAccessForOperators(true);
        mvc.perform(get(auditPath(A)).with(onHost(TestTenants.GLOBAL_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(operatorToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void anAuditEntryOlderThanTheRetentionIsNotListed() throws Exception {
        String sessionId = sessionIdOf(openSession(A, superAdminToken, REASON));
        TenantDatabase.admin().sql("""
                        UPDATE global.access_audit_event SET occurred_at = now() - interval '366 days'
                        WHERE session_id = :session::uuid
                        """)
                .param("session", sessionId)
                .update();

        JsonNode entries = auditEntriesOf(A);
        for (JsonNode entry : entries) {
            assertThat(entry.get("sessionId").isNull() || !entry.get("sessionId").asString().equals(sessionId))
                    .isTrue();
        }
    }

    @Test
    void unknownComplexesForbiddenCallersAndMalformedPagesAreAnswered() throws Exception {
        TenantId unknown = new TenantId(UUID.randomUUID().toString());
        openSession(unknown, superAdminToken, REASON)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPLEX_NOT_FOUND"));
        mvc.perform(get(auditPath(unknown)).with(onHost(TestTenants.GLOBAL_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPLEX_NOT_FOUND"));
        mvc.perform(get(auditPath(A)).with(onHost(TestTenants.GLOBAL_HOST)).param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        String playerEmail = TestAccounts.uniqueEmail("player");
        TestAccounts.player(playerEmail);
        openSession(A, logIn(PLAYER_LOGIN, playerEmail), REASON)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        openSession(A, null, REASON)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void supportOperationsAreNotServedOnTheHostOfAComplex() throws Exception {
        mvc.perform(post(sessionsPath(A)).with(onHost(TestTenants.hostOf(A)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reasonBody(REASON)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /** The backoffice preflight reaches active and suspended complexes; other origins are refused. */
    @Test
    void corsAdmitsOnlyTheBackofficeOriginWithTheSupportHeader() throws Exception {
        for (TenantId tenant : new TenantId[] {A, SUSPENDED}) {
            mvc.perform(preflight(tenant, BACKOFFICE_ORIGIN))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, BACKOFFICE_ORIGIN));
        }
        mvc.perform(preflight(A, "https://evil.example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private MockHttpServletRequestBuilder preflight(TenantId tenant, String origin) {
        return options(PROBE).with(onHost(TestTenants.hostOf(tenant)))
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization," + SESSION_HEADER);
    }

    private ResultActions openSession(TenantId tenant, String token, String reason) throws Exception {
        MockHttpServletRequestBuilder request = post(sessionsPath(tenant)).with(onHost(TestTenants.GLOBAL_HOST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reasonBody(reason));
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        return mvc.perform(request);
    }

    private ResultActions closeSession(String sessionId, String token) throws Exception {
        return mvc.perform(delete("/api/v1/platform/support-sessions/" + sessionId)
                .with(onHost(TestTenants.GLOBAL_HOST))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private static MockHttpServletRequestBuilder listStaffBookings() {
        return get(STAFF_BOOKINGS).param("from", "2030-01-01T00:00:00Z").param("to", "2030-01-02T00:00:00Z");
    }

    private ResultActions probeOn(TenantId tenant, String token, String sessionId) throws Exception {
        return onTenant(get(PROBE), tenant, token, sessionId);
    }

    private ResultActions onTenant(MockHttpServletRequestBuilder request, TenantId tenant, String token,
            String sessionId) throws Exception {
        request.with(onHost(TestTenants.hostOf(tenant)));
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        if (sessionId != null) {
            request.header(SESSION_HEADER, sessionId);
        }
        return mvc.perform(request);
    }

    private JsonNode auditEntriesOf(TenantId tenant) throws Exception {
        String page = mvc.perform(get(auditPath(tenant)).with(onHost(TestTenants.GLOBAL_HOST))
                        .param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(page).get("items");
    }

    private String logIn(String path, String email) throws Exception {
        String response = mvc.perform(post(path).with(onHost(TestTenants.GLOBAL_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, TestAccounts.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("accessToken").asString();
    }

    private static String sessionIdOf(ResultActions opened) throws Exception {
        return JSON.readTree(opened.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("sessionId").asString();
    }

    private static String sessionsPath(TenantId tenant) {
        return "/api/v1/platform/complexes/" + tenant.value() + "/support-sessions";
    }

    private static String auditPath(TenantId tenant) {
        return "/api/v1/platform/complexes/" + tenant.value() + "/audit-entries";
    }

    private static String reasonBody(String reason) {
        return JSON.writeValueAsString(Map.of("reason", reason));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static void setStatus(TenantId tenant, String status) {
        TenantDatabase.admin().sql("UPDATE global.tenant SET status = :status WHERE id = :id::uuid")
                .param("status", status)
                .param("id", tenant.value())
                .update();
    }

    /** Sets TENANT_DATA_ACCESS for operators directly, as a change of the SUPER_ADMIN would. */
    private static void tenantDataAccessForOperators(boolean enabled) {
        TenantDatabase.admin().sql("""
                        UPDATE global.operator_capability SET enabled = :enabled, version = version + 1
                        WHERE capability_key = 'TENANT_DATA_ACCESS' AND enabled <> :enabled
                        """)
                .param("enabled", enabled)
                .update();
    }

    private static long sessionsOf(UUID platformUserId) {
        return TenantDatabase.admin()
                .sql("SELECT count(*) FROM global.access_session WHERE platform_user_id = :id")
                .param("id", platformUserId)
                .query(Long.class)
                .single();
    }

    private static long auditRowsOutsideTenant(TenantId tenant, String sessionId) {
        return TenantDatabase.admin()
                .sql("""
                        SELECT count(*) FROM global.access_audit_event
                        WHERE session_id = :session::uuid AND tenant_id <> :tenant::uuid
                        """)
                .param("session", sessionId)
                .param("tenant", tenant.value())
                .query(Long.class)
                .single();
    }

    /** Makes inserts of allowed accesses fail, as if the audit store were unavailable for them. */
    private static void failAllowedAuditInserts() {
        JdbcClient admin = TenantDatabase.admin();
        admin.sql("""
                CREATE OR REPLACE FUNCTION global.test_fail_audit() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    RAISE EXCEPTION 'audit store unavailable (test)';
                END
                $$
                """).update();
        admin.sql("""
                CREATE TRIGGER test_fail_allowed_audit BEFORE INSERT ON global.access_audit_event
                FOR EACH ROW WHEN (NEW.outcome = 'ALLOWED') EXECUTE FUNCTION global.test_fail_audit()
                """).update();
    }

    private static void restoreAuditInserts() {
        JdbcClient admin = TenantDatabase.admin();
        admin.sql("DROP TRIGGER IF EXISTS test_fail_allowed_audit ON global.access_audit_event").update();
        admin.sql("DROP FUNCTION IF EXISTS global.test_fail_audit()").update();
    }

    /**
     * Tenants A, B and a third one to suspend, provisioned, plus an operational endpoint of the tenant host that
     * reports the tenant scope and the schema of its connection, and counts how many times it ran.
     */
    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class ProbeOnTenantHost {

        private final AtomicInteger runs = new AtomicInteger();
        private final JdbcClient tenantData;

        ProbeOnTenantHost(DataSource tenantData) {
            this.tenantData = JdbcClient.create(tenantData);
        }

        @Bean
        @DependsOn(FLYWAY_INITIALIZER)
        TenantId[] provisionedTenants(TenantMigrations migrations) {
            return new TenantId[] {
                TestTenants.provision(migrations, A), TestTenants.provision(migrations, B),
                TestTenants.provision(migrations, SUSPENDED)};
        }

        @GetMapping(PROBE)
        Map<String, String> schema() {
            runs.incrementAndGet();
            String schema = tenantData.sql("SELECT current_schema()").query(String.class).single();
            return Map.of("tenant", TenantScope.require().value(), "schema", schema);
        }

        int runs() {
            return runs.get();
        }
    }
}
