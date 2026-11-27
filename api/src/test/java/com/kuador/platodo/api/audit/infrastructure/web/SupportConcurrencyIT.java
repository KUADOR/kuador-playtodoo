package com.kuador.platodo.api.audit.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static com.kuador.platodo.api.support.TestTenants.FLYWAY_INITIALIZER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnPlayerApiEnabled;
import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnStaffApiEnabled;
import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.PurgeDueTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;
import com.kuador.platodo.api.tenancy.infrastructure.scheduling.PurgeDueTenantsJob;

import tools.jackson.databind.json.JsonMapper;

/**
 * Concurrency of Kuador support (AGENTS.md, ADR-006, ADR-020): support and a player booking the same slot of an
 * active complex, two support sessions blocking the same slot of a suspended complex, and support requests racing the
 * purge of an archived complex. Real HTTP stack and PostgreSQL; only the court rules are a test double, because the
 * Courts module does not exist yet.
 */
@SpringBootTest(properties = {
        ConditionalOnStaffApiEnabled.PROPERTY + "=true",
        ConditionalOnPlayerApiEnabled.PROPERTY + "=true"})
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class SupportConcurrencyIT {

    private static final TenantId ACTIVE = new TenantId("0e880000-0000-4000-8000-000000000002");
    private static final TenantId SUSPENDED = new TenantId("0e880000-0000-4000-8000-000000000003");
    private static final String SESSION_HEADER = "X-Support-Session";
    private static final String PLATFORM_LOGIN = "/api/v1/auth/platform/login";
    private static final String PLAYER_LOGIN = "/api/v1/auth/players/login";
    private static final String REASON = "Duplicated booking complaint, ticket 1234";
    private static final Duration SLOT = Duration.ofMinutes(90);
    private static final int CREATED = 201;
    private static final int CONFLICT = 409;
    /**
     * Below the connection pool of the test context (10): each racing request may wait for the tenant row the purge
     * holds while keeping its own connection, so more requests than connections would only measure the pool.
     */
    private static final int RACING_REQUESTS = 6;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TestConfiguration(proxyBeanMethods = false)
    static class Doubles {

        @Bean
        @DependsOn(FLYWAY_INITIALIZER)
        TenantId[] provisionedTenants(TenantMigrations migrations) {
            TenantId[] tenants = {TestTenants.provision(migrations, ACTIVE),
                TestTenants.provision(migrations, SUSPENDED)};
            TenantDatabase.admin().sql("UPDATE global.tenant SET status = 'SUSPENDED' WHERE id = :id::uuid")
                    .param("id", SUSPENDED.value())
                    .update();
            return tenants;
        }

        @Bean
        StubCourtSlotRules courtSlotRules() {
            return new StubCourtSlotRules();
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StubCourtSlotRules courts;
    @Autowired
    private TenantMigrations migrations;
    @Autowired
    private PurgeDueTenants purge;

    private UUID court;
    private Instant start;

    @BeforeEach
    void setUp() {
        court = courts.addCourt(SLOT);
        start = Instant.now().plus(Duration.ofDays(2)).truncatedTo(ChronoUnit.HOURS);
    }

    /** Support and a player take the same slot of an active complex at once: exactly one gets it. */
    @Test
    void supportAndAPlayerNeverBothBookTheSameSlot() throws Exception {
        String superAdmin = superAdminToken();
        String session = openSession(ACTIVE, superAdmin);
        String player = playerToken();

        List<Integer> statuses = race(
                () -> send(post("/api/v1/staff/bookings"), ACTIVE, superAdmin, session, """
                        {"courtId": "%s", "start": "%s", "holderName": "Alex Example"}
                        """.formatted(court, start)),
                () -> send(post("/api/v1/bookings"), ACTIVE, player, null, """
                        {"courtId": "%s", "start": "%s"}
                        """.formatted(court, start)));

        assertThat(statuses).containsExactlyInAnyOrder(CREATED, CONFLICT);
        assertThat(activeEntriesOn(ACTIVE, court)).isOne();
    }

    /** CA13: two SUPER_ADMIN sessions block the same slot of a suspended complex at once; one inactive block stays. */
    @Test
    void twoSupportSessionsOnASuspendedComplexNeverBothBlockTheSameSlot() throws Exception {
        String first = superAdminToken();
        String second = superAdminToken();
        String firstSession = openSession(SUSPENDED, first);
        String secondSession = openSession(SUSPENDED, second);
        String block = """
                {"courtId": "%s", "start": "%s", "reason": "Maintenance"}
                """.formatted(court, start);

        List<Integer> statuses = race(
                () -> send(post("/api/v1/staff/blocks"), SUSPENDED, first, firstSession, block),
                () -> send(post("/api/v1/staff/blocks"), SUSPENDED, second, secondSession, block));

        assertThat(statuses).containsExactlyInAnyOrder(CREATED, CONFLICT);
        assertThat(activeEntriesOn(SUSPENDED, court)).isOne();
        MockHttpServletResponse listed = mvc.perform(get("/api/v1/staff/bookings")
                        .with(onHost(TestTenants.hostOf(SUSPENDED)))
                        .param("from", start.toString()).param("to", start.plus(SLOT).toString())
                        .param("courtId", court.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(first))
                        .header(SESSION_HEADER, firstSession))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        assertThat(JSON.readTree(listed.getContentAsString()).get(0).get("inactive").asBoolean()).isTrue();
    }

    /**
     * Support requests racing the purge: each is either served before the schema is gone or refused; none fails, and
     * once purged the complex answers "not found" and its sessions are closed. The audit insert of a racing request
     * holds a key-share lock on the tenant row, so the purge may skip the complex in that run (it never waits on a
     * held row) and finish it in the next one; from PURGING on no request reaches it.
     */
    @Test
    void supportRequestsRacingThePurgeAreServedOrRefusedNeverBroken() throws Exception {
        TenantId archived = TestTenants.provision(migrations, new TenantId(UUID.randomUUID().toString()));
        archiveOverdue(archived);
        String superAdmin = superAdminToken();
        String session = openSession(archived, superAdmin);

        List<Callable<Integer>> work = new ArrayList<>();
        for (int i = 0; i < RACING_REQUESTS; i++) {
            work.add(() -> send(get("/api/v1/staff/bookings")
                    .param("from", start.toString()).param("to", start.plus(SLOT).toString()),
                    archived, superAdmin, session, null));
        }
        work.add(() -> {
            new PurgeDueTenantsJob(purge).run();
            return 0;
        });

        List<Integer> statuses = race(work);

        assertThat(statuses.subList(0, RACING_REQUESTS)).allSatisfy(status -> assertThat(status).isIn(200, 403, 404));
        assertThat(tenantStatusOf(archived)).isIn("PURGING", "PURGED");
        new PurgeDueTenantsJob(purge).run();
        assertThat(tenantStatusOf(archived)).isEqualTo("PURGED");
        assertThat(send(get("/api/v1/staff/bookings"), archived, superAdmin, session, null)).isEqualTo(404);
        assertThat(openSessionsOf(archived)).isZero();
    }

    @SafeVarargs
    private List<Integer> race(Callable<Integer>... requests) throws Exception {
        return race(List.of(requests));
    }

    /** Starts every request at the same time and returns their statuses in the order given. */
    private List<Integer> race(List<Callable<Integer>> requests) throws Exception {
        CyclicBarrier start = new CyclicBarrier(requests.size());
        List<Future<Integer>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(requests.size())) {
            for (Callable<Integer> request : requests) {
                results.add(executor.submit(() -> {
                    start.await();
                    return request.call();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(1, TimeUnit.MINUTES));
            }
            return statuses;
        }
    }

    private int send(MockHttpServletRequestBuilder request, TenantId tenant, String token, String session,
            String body) throws Exception {
        request.with(onHost(TestTenants.hostOf(tenant))).header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (session != null) {
            request.header(SESSION_HEADER, session);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private String openSession(TenantId tenant, String token) throws Exception {
        String opened = mvc.perform(post("/api/v1/platform/complexes/" + tenant.value() + "/support-sessions")
                        .with(onHost(TestTenants.GLOBAL_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("reason", REASON))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(opened).get("sessionId").asString();
    }

    private String superAdminToken() throws Exception {
        String email = TestAccounts.uniqueEmail("super-admin");
        TestAccounts.platformOwner(email);
        return logIn(PLATFORM_LOGIN, email);
    }

    private String playerToken() throws Exception {
        String email = TestAccounts.uniqueEmail("player");
        TestAccounts.player(email);
        return logIn(PLAYER_LOGIN, email);
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

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static long activeEntriesOn(TenantId tenant, UUID court) {
        return TenantDatabase.admin()
                .sql("""
                        SELECT count(*) FROM "%s".booking
                        WHERE court_id = :court AND status IN ('PENDING', 'CONFIRMED')
                        """.formatted(TestTenants.schemaOf(tenant)))
                .param("court", court)
                .query(Long.class)
                .single();
    }

    /** Cancelled while active with a purge date already reached, as the purge job finds it (ADR-014, ADR-015). */
    private static void archiveOverdue(TenantId tenant) {
        TenantDatabase.admin().sql("""
                        UPDATE global.tenant SET status = 'ARCHIVED', archived_at = now() - interval '46 days',
                            retention_days = 45, purge_due_at = now() - interval '1 day'
                        WHERE id = :id::uuid
                        """)
                .param("id", tenant.value())
                .update();
    }

    private static String tenantStatusOf(TenantId tenant) {
        return TenantDatabase.admin().sql("SELECT status FROM global.tenant WHERE id = :id::uuid")
                .param("id", tenant.value())
                .query(String.class)
                .single();
    }

    private static long openSessionsOf(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT count(*) FROM global.access_session WHERE tenant_id = :id::uuid AND closed_at IS NULL")
                .param("id", tenant.value())
                .query(Long.class)
                .single();
    }
}
