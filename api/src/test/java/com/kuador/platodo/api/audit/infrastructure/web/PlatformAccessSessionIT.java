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

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Audited access sessions of the Kuador owner (ADR-011; PLATODO-26 criteria 10, 11 and 20), over HTTP with the real
 * tenant resolution, token filter, platform access guard and PostgreSQL. A probe endpoint of the tenant host stands
 * in for the operational endpoints: it reports the schema its connection uses and counts its own runs.
 */
@SpringBootTest
@Import(PlatformAccessSessionIT.ProbeOnTenantHost.class)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class PlatformAccessSessionIT {

    private static final TenantId A = TestTenants.A;
    private static final TenantId B = TestTenants.B;
    private static final String PROBE = "/api/v1/test-probe/schema";
    private static final String PLATFORM_LOGIN = "/api/v1/auth/platform/login";
    private static final String PLAYER_LOGIN = "/api/v1/auth/players/login";
    private static final String SESSION_HEADER = "X-Platform-Access-Session";
    private static final String BACKOFFICE_ORIGIN = "http://admin.localhost:8000";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String REASON = "Complaint about a duplicated booking, ticket 1234";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccessTokens accessTokens;
    @Autowired
    private ProbeOnTenantHost probe;

    private UUID ownerId;
    private String ownerToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = TestAccounts.uniqueEmail("owner");
        ownerId = TestAccounts.platformOwner(email);
        ownerToken = logIn(PLATFORM_LOGIN, email);
    }

    /** Criterion 11: with a reason the owner reaches complex A, audited first and only inside the schema of A. */
    @Test
    void withAReasonTheOwnerReachesTheComplexAuditedAndOnlyInItsSchema() throws Exception {
        String session = openSession(A, ownerToken, REASON)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantHost").value(TestTenants.hostOf(A)))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        String sessionId = JSON.readTree(session).get("sessionId").asString();
        Instant expiresAt = Instant.parse(JSON.readTree(session).get("expiresAt").asString());
        assertThat(Duration.between(Instant.now(), expiresAt)).isBetween(Duration.ofMinutes(29), Duration.ofMinutes(30));

        mvc.perform(get(PROBE).with(onHost(TestTenants.hostOf(A)))
                        .param("email", "someone@example.com")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant").value(A.value()))
                .andExpect(jsonPath("$.schema").value(TestTenants.schemaOf(A)));

        JsonNode event = latestAuditEventOf(A);
        assertThat(event.get("outcome").asString()).isEqualTo("ALLOWED");
        assertThat(event.get("sessionId").asString()).isEqualTo(sessionId);
        assertThat(event.get("platformUserId").asString()).isEqualTo(ownerId.toString());
        assertThat(event.get("reason").asString()).isEqualTo(REASON);
        assertThat(event.get("reasonRedacted").asBoolean()).isFalse();
        assertThat(event.get("httpMethod").asString()).isEqualTo("GET");
        assertThat(event.get("routeTemplate").asString()).isEqualTo(PROBE);
    }

    @Test
    void theOwnerReadsTheStateOfTheComplexWithASession() throws Exception {
        String sessionId = sessionIdOf(openSession(A, ownerToken, REASON));

        mvc.perform(get("/api/v1/tenant").with(onHost(TestTenants.hostOf(A)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    /** Criterion 11: without a reason no session is opened. */
    @Test
    void withoutAReasonNoSessionIsOpened() throws Exception {
        long sessionsBefore = sessionsOf(ownerId);

        openSession(A, ownerToken, "   ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        mvc.perform(post(sessionsPath(A)).with(onHost(TestTenants.GLOBAL_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));

        assertThat(sessionsOf(ownerId)).isEqualTo(sessionsBefore);
    }

    @Test
    void aReasonLongerThanTheContractLimitIsAnInvalidRequest() throws Exception {
        openSession(A, ownerToken, "x".repeat(501))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /** Criterion 10: a Kuador role other than the owner is refused (403), as is any other kind of account. */
    @Test
    void aCallerThatIsNotTheOwnerIsForbidden() throws Exception {
        String nonOwnerToken = accessTokens.issue(
                AuthenticatedPrincipal.global(ownerId, AccountType.PLATFORM, "PLATFORM_SUPPORT")).value();
        String playerEmail = TestAccounts.uniqueEmail("player");
        TestAccounts.player(playerEmail);
        String playerToken = logIn(PLAYER_LOGIN, playerEmail);

        for (String token : new String[] {nonOwnerToken, playerToken}) {
            openSession(A, token, REASON)
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            mvc.perform(get(auditPath(A)).with(onHost(TestTenants.GLOBAL_HOST))
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        openSession(A, null, REASON)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    /** Criterion 10: even holding the id of a valid session, a non-owner Kuador role never reaches the complex. */
    @Test
    void aNonOwnerPlatformRoleIsDeniedOnTheComplexEvenWithAValidSession() throws Exception {
        String sessionId = sessionIdOf(openSession(A, ownerToken, REASON));
        String nonOwnerToken = accessTokens.issue(
                AuthenticatedPrincipal.global(ownerId, AccountType.PLATFORM, "PLATFORM_SUPPORT")).value();
        int runsBefore = probe.runs();

        probeOn(A, nonOwnerToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_SESSION_INVALID"));

        assertThat(probe.runs()).isEqualTo(runsBefore);
        assertThat(latestAuditEventOf(A).get("outcome").asString()).isEqualTo("DENIED");
    }

    /** A session opened for A is rejected on the host of B; the denial is audited on B without A's reason. */
    @Test
    void theSessionOfAUsedOnBIsRejected() throws Exception {
        String sessionId = sessionIdOf(openSession(A, ownerToken, REASON));
        int runsBefore = probe.runs();

        probeOn(B, ownerToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_SESSION_INVALID"));

        assertThat(probe.runs()).isEqualTo(runsBefore);
        JsonNode denial = latestAuditEventOf(B);
        assertThat(denial.get("outcome").asString()).isEqualTo("DENIED");
        assertThat(denial.get("platformUserId").asString()).isEqualTo(ownerId.toString());
        assertThat(denial.get("sessionId").isNull()).isTrue();
        assertThat(denial.get("reason").isNull()).isTrue();
        assertThat(denial.get("routeTemplate").asString()).isEqualTo(PROBE);
    }

    @Test
    void aPlatformTokenWithoutSessionIsRejectedOnTheHostOfAComplex() throws Exception {
        int runsBefore = probe.runs();

        probeOn(A, ownerToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_SESSION_INVALID"));
        probeOn(A, ownerToken, "not-a-session-id")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_SESSION_INVALID"));

        assertThat(probe.runs()).isEqualTo(runsBefore);
    }

    @Test
    void aClosedSessionNoLongerGrantsAccessAndClosingIsIdempotent() throws Exception {
        String sessionId = sessionIdOf(openSession(A, ownerToken, REASON));
        probeOn(A, ownerToken, sessionId).andExpect(status().isOk());

        closeSession(sessionId, ownerToken).andExpect(status().isNoContent());
        closeSession(sessionId, ownerToken).andExpect(status().isNoContent());

        probeOn(A, ownerToken, sessionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_SESSION_INVALID"));
    }

    @Test
    void onlyTheOwnerWhoOpenedASessionCanCloseIt() throws Exception {
        String sessionId = sessionIdOf(openSession(A, ownerToken, REASON));
        String otherEmail = TestAccounts.uniqueEmail("other-owner");
        TestAccounts.platformOwner(otherEmail);
        String otherOwnerToken = logIn(PLATFORM_LOGIN, otherEmail);

        closeSession(sessionId, otherOwnerToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCESS_SESSION_NOT_FOUND"));
        closeSession(UUID.randomUUID().toString(), ownerToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCESS_SESSION_NOT_FOUND"));
        probeOn(A, otherOwnerToken, sessionId).andExpect(status().isForbidden());
        probeOn(A, ownerToken, sessionId).andExpect(status().isOk());
    }

    /** Criterion 20: when the audit cannot be written, a valid session is denied and the handler never runs. */
    @Test
    void whenTheAuditCannotBeWrittenTheAccessIsDenied() throws Exception {
        String sessionId = sessionIdOf(openSession(A, ownerToken, REASON));
        int runsBefore = probe.runs();
        failAllowedAuditInserts();
        try {
            probeOn(A, ownerToken, sessionId)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_SESSION_INVALID"));
        } finally {
            restoreAuditInserts();
        }

        assertThat(probe.runs()).isEqualTo(runsBefore);
        probeOn(A, ownerToken, sessionId).andExpect(status().isOk());
    }

    @Test
    void theAuditIsPagedNewestFirstAndOnlyForThatComplex() throws Exception {
        String sessionId = sessionIdOf(openSession(A, ownerToken, REASON));
        probeOn(A, ownerToken, sessionId).andExpect(status().isOk());
        probeOn(A, ownerToken, null).andExpect(status().isForbidden());

        String page = mvc.perform(get(auditPath(A)).with(onHost(TestTenants.GLOBAL_HOST))
                        .param("page", "0").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].outcome").value("DENIED"))
                .andExpect(jsonPath("$.items[1].outcome").value("ALLOWED"))
                .andReturn().getResponse().getContentAsString();

        assertThat(JSON.readTree(page).get("totalItems").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(auditRowsOutsideTenant(A, sessionId)).isZero();
    }

    @Test
    void theAuditOfAnUnknownComplexIsNotFoundAndPagesOutOfRangeAreInvalid() throws Exception {
        mvc.perform(get(auditPath(new TenantId(UUID.randomUUID().toString()))).with(onHost(TestTenants.GLOBAL_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_NOT_FOUND"));
        mvc.perform(get(auditPath(A)).with(onHost(TestTenants.GLOBAL_HOST)).param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        openSession(new TenantId(UUID.randomUUID().toString()), ownerToken, REASON)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_NOT_FOUND"));
    }

    @Test
    void platformAccessOperationsAreNotServedOnTheHostOfAComplex() throws Exception {
        mvc.perform(post(sessionsPath(A)).with(onHost(TestTenants.hostOf(A)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reasonBody(REASON)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void corsAdmitsOnlyTheBackofficeOriginWithTheSessionHeader() throws Exception {
        mvc.perform(preflight(BACKOFFICE_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, BACKOFFICE_ORIGIN));
        mvc.perform(preflight("https://evil.example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private MockHttpServletRequestBuilder preflight(String origin) {
        return options(PROBE).with(onHost(TestTenants.hostOf(A)))
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
        return mvc.perform(delete("/api/v1/platform/access-sessions/" + sessionId)
                .with(onHost(TestTenants.GLOBAL_HOST))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions probeOn(TenantId tenant, String token, String sessionId) throws Exception {
        MockHttpServletRequestBuilder request = get(PROBE).with(onHost(TestTenants.hostOf(tenant)))
                .header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (sessionId != null) {
            request.header(SESSION_HEADER, sessionId);
        }
        return mvc.perform(request);
    }

    private JsonNode latestAuditEventOf(TenantId tenant) throws Exception {
        String page = mvc.perform(get(auditPath(tenant)).with(onHost(TestTenants.GLOBAL_HOST))
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(page).get("items").get(0);
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
        return "/api/v1/platform/tenants/" + tenant.value() + "/access-sessions";
    }

    private static String auditPath(TenantId tenant) {
        return "/api/v1/platform/tenants/" + tenant.value() + "/access-audit";
    }

    private static String reasonBody(String reason) {
        return JSON.writeValueAsString(Map.of("reason", reason));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
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
     * Tenants A and B provisioned, plus an operational endpoint of the tenant host that reports the tenant scope and
     * the schema of its connection, and counts how many times it ran.
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
                TestTenants.provision(migrations, A), TestTenants.provision(migrations, B)};
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
