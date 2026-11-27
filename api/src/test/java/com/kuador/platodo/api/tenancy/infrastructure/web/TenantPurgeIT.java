package com.kuador.platodo.api.tenancy.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.PurgeDueTenants;
import com.kuador.platodo.api.tenancy.application.TenancyUseCases;
import com.kuador.platodo.api.tenancy.application.TenantLifecycle;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.InvalidTransitionException;
import com.kuador.platodo.api.tenancy.domain.event.TenantPurged;
import com.kuador.platodo.api.tenancy.infrastructure.scheduling.PurgeDueTenantsJob;

/**
 * Permanent deletion of an archived complex (ADR-013: D8, D10, D11) with the real wiring and PostgreSQL. Numbers refer
 * to the criteria of PLATODO-26. The database is shared with other test classes, so every assertion is about the
 * complexes this class registers, never about totals.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class TenantPurgeIT {

    private static final String REDACTED_REASON = "[anonimizado por baja definitiva]";
    private static final String ACCESS_REASON = "Soporte: revisar reservas del cliente Ana";
    private static final String OUTBOX_TYPE = "TENANT_PERMANENTLY_DELETED";
    private static final Duration OVERDUE = Duration.ofDays(1);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private PurgeDueTenants purge;
    @Autowired
    private PurgeEvents purgeEvents;
    @Autowired
    private TenancyUseCases useCases;

    private TenancyRequests api;
    private String ownerToken;
    private TenantLifecycle lifecycle;

    @BeforeEach
    void setUp() throws Exception {
        api = new TenancyRequests(mvc);
        ownerToken = api.platformOwnerToken();
        lifecycle = useCases.lifecycle();
    }

    /** Criteria 13 and 16: the owner is told, the schema is deleted and only the anonymized global records remain. */
    @Test
    void anArchivedComplexPastItsPurgeDateIsPermanentlyDeleted() throws Exception {
        String subdomain = TenancyRequests.uniqueSubdomain("purge");
        String ownerEmail = TestAccounts.uniqueEmail("owner");
        TenantId purged = registered(subdomain, ownerEmail);
        TenantId kept = api.registered(ownerToken, TenancyRequests.uniqueSubdomain("kept"));
        UUID kuadorUser = TestAccounts.platformOwner(TestAccounts.uniqueEmail("kuador"));
        auditedAccess(kuadorUser, purged);
        auditedAccess(kuadorUser, kept);
        archiveOverdue(purged);

        runJob();

        assertThat(tenantRow(purged)).containsEntry("status", "PURGED")
                .containsEntry("display_name", "Complex " + subdomain);
        assertThat(tenantRow(purged).get("purged_at")).isNotNull();
        assertThat(tenantRow(purged).get("owner_email")).isNull();
        assertThat(schemaExists(purged)).isFalse();
        assertThat(domainRow(subdomain)).containsEntry("is_current", false);
        assertThat(domainRow(subdomain).get("retired_at")).isNotNull();
        api.tenantState(subdomain).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_NOT_FOUND"));
        api.availability(subdomain).andExpect(jsonPath("$.available").value(false));
        assertThat(outboxFor(purged)).singleElement().satisfies(message -> {
            assertThat(message).containsEntry("type", OUTBOX_TYPE).containsEntry("recipient", ownerEmail);
            assertThat(message.get("display_name").toString()).isEqualTo("Complex " + subdomain);
        });
        assertThat(sessionReasonsOf(purged)).containsExactly(REDACTED_REASON);
        assertThat(eventReasonsOf(purged)).containsExactlyInAnyOrder(REDACTED_REASON, null);
        assertThat(unredactedRowsOf(purged)).isZero();
        assertThat(purgeEvents.countFor(purged)).isEqualTo(1);
    }

    /** AGENTS.md isolation: purging one complex leaves every other complex and its audit untouched. */
    @Test
    void thePurgeOfOneComplexNeverTouchesAnother() throws Exception {
        TenantId purged = registered(TenancyRequests.uniqueSubdomain("purge-iso"), TestAccounts.uniqueEmail("o"));
        String keptSubdomain = TenancyRequests.uniqueSubdomain("kept-iso");
        TenantId kept = api.registered(ownerToken, keptSubdomain);
        UUID kuadorUser = TestAccounts.platformOwner(TestAccounts.uniqueEmail("kuador"));
        auditedAccess(kuadorUser, purged);
        auditedAccess(kuadorUser, kept);
        archiveOverdue(purged);

        runJob();

        assertThat(tenantRow(kept)).containsEntry("status", "ACTIVE");
        assertThat(tenantRow(kept).get("owner_email")).isNotNull();
        assertThat(schemaExists(kept)).isTrue();
        api.tenantState(keptSubdomain).andExpect(status().isOk());
        assertThat(sessionReasonsOf(kept)).containsExactly(ACCESS_REASON);
        assertThat(eventReasonsOf(kept)).containsExactlyInAnyOrder(ACCESS_REASON, null);
        assertThat(outboxFor(kept)).isEmpty();
        assertThat(purgeEvents.countFor(kept)).isZero();
    }

    /** Criterion 13: never before the purge date. */
    @Test
    void anArchivedComplexIsNotPurgedBeforeItsPurgeDate() throws Exception {
        TenantId archived = registered(TenancyRequests.uniqueSubdomain("early"), TestAccounts.uniqueEmail("o"));
        lifecycle.cancel(archived, Instant.now());

        runJob();

        assertNotPurged(archived, "ARCHIVED");
    }

    /** Criterion 13: never outside ARCHIVED, even with a purge date in the past. */
    @Test
    void onlyArchivedComplexesArePurged() throws Exception {
        TenantId active = registered(TenancyRequests.uniqueSubdomain("active"), TestAccounts.uniqueEmail("o"));
        TenantId suspended = registered(TenancyRequests.uniqueSubdomain("susp"), TestAccounts.uniqueEmail("o"));
        lifecycle.suspend(suspended, Instant.now());
        forcePurgeDate(active, Instant.now().minus(OVERDUE));
        forcePurgeDate(suspended, Instant.now().minus(OVERDUE));

        runJob();

        assertNotPurged(active, "ACTIVE");
        assertNotPurged(suspended, "SUSPENDED");
    }

    /** A failure leaves the complex PURGING with all its data; the retry purges it once, with one e-mail. */
    @Test
    void aFailedPurgeRollsBackAndIsRetriedIdempotently() throws Exception {
        String subdomain = TenancyRequests.uniqueSubdomain("retry");
        TenantId tenant = registered(subdomain, TestAccounts.uniqueEmail("owner"));
        auditedAccess(TestAccounts.platformOwner(TestAccounts.uniqueEmail("kuador")), tenant);
        archiveOverdue(tenant);
        purgeEvents.failFor(tenant);

        runJob();

        assertThat(tenantRow(tenant)).containsEntry("status", "PURGING");
        assertThat(schemaExists(tenant)).isTrue();
        assertThat(domainRow(subdomain)).containsEntry("is_current", true);
        assertThat(outboxFor(tenant)).isEmpty();
        assertThat(sessionReasonsOf(tenant)).containsExactly(ACCESS_REASON);
        api.tenantState(subdomain).andExpect(status().isNotFound());
        assertThatThrownBy(() -> lifecycle.reactivate(tenant, Instant.now()))
                .isInstanceOf(InvalidTransitionException.class);

        purgeEvents.stopFailing();
        runJob();
        runJob();

        assertThat(tenantRow(tenant)).containsEntry("status", "PURGED");
        assertThat(schemaExists(tenant)).isFalse();
        assertThat(outboxFor(tenant)).hasSize(1);
        assertThat(sessionReasonsOf(tenant)).containsExactly(REDACTED_REASON);
    }

    /** D8: two api instances running the job at once purge the complex once. */
    @Test
    void twoInstancesRunningTogetherPurgeTheComplexOnce() throws Exception {
        TenantId tenant = registered(TenancyRequests.uniqueSubdomain("twice"), TestAccounts.uniqueEmail("owner"));
        archiveOverdue(tenant);
        PurgeDueTenantsJob first = new PurgeDueTenantsJob(purge);
        PurgeDueTenantsJob second = new PurgeDueTenantsJob(purge);
        CyclicBarrier start = new CyclicBarrier(2);

        try (ExecutorService instances = Executors.newFixedThreadPool(2)) {
            List<Future<?>> runs = List.of(
                    instances.submit(() -> runAfter(start, first)),
                    instances.submit(() -> runAfter(start, second)));
            for (Future<?> run : runs) {
                run.get();
            }
        }

        assertThat(tenantRow(tenant)).containsEntry("status", "PURGED");
        assertThat(outboxFor(tenant)).hasSize(1);
        assertThat(purgeEvents.countFor(tenant)).isEqualTo(1);
    }

    /** D8: a complex another instance holds is skipped at once, neither claimed nor purged twice. */
    @Test
    void aComplexHeldByAnotherInstanceIsSkippedWithoutWaiting() throws Exception {
        TenantId tenant = registered(TenancyRequests.uniqueSubdomain("held"), TestAccounts.uniqueEmail("owner"));
        archiveOverdue(tenant);

        try (Connection otherInstance = TenantDatabase.tenantDataSource("global").getConnection()) {
            holdTenantRow(otherInstance, tenant);

            purge.claimDue();
            assertThat(tenantRow(tenant)).containsEntry("status", "ARCHIVED");

            otherInstance.rollback();
            purge.claimDue();
            holdTenantRow(otherInstance, tenant);
            assertThat(purge.purge(tenant)).isFalse();
            otherInstance.rollback();
        }

        assertThat(tenantRow(tenant)).containsEntry("status", "PURGING");
        assertThat(purge.purge(tenant)).isTrue();
        assertThat(purge.purge(tenant)).isFalse();
        assertThat(outboxFor(tenant)).hasSize(1);
    }

    /** A schema already gone leaves nobody to tell: the purge still ends instead of failing on every run. */
    @Test
    void aComplexWhoseSchemaIsAlreadyGoneIsStillPurged() throws Exception {
        TenantId tenant = registered(TenancyRequests.uniqueSubdomain("gone"), TestAccounts.uniqueEmail("owner"));
        archiveOverdue(tenant);
        TenantDatabase.admin().sql("DROP SCHEMA \"" + TestTenants.schemaOf(tenant) + "\" CASCADE").update();

        runJob();

        assertThat(tenantRow(tenant)).containsEntry("status", "PURGED");
        assertThat(outboxFor(tenant)).isEmpty();
        assertThat(purgeEvents.countFor(tenant)).isEqualTo(1);
    }

    private TenantId registered(String subdomain, String ownerEmail) throws Exception {
        return new TenantId(TenancyRequests.json(api.register(ownerToken, "Complex " + subdomain, subdomain, ownerEmail)
                .andExpect(status().isCreated())).get("id").asString());
    }

    private void archiveOverdue(TenantId tenant) throws Exception {
        lifecycle.cancel(tenant, Instant.now());
        forcePurgeDate(tenant, Instant.now().minus(OVERDUE));
    }

    private void runJob() {
        new PurgeDueTenantsJob(purge).run();
    }

    private static Void runAfter(CyclicBarrier start, PurgeDueTenantsJob job) throws Exception {
        start.await();
        job.run();
        return null;
    }

    private void assertNotPurged(TenantId tenant, String status) {
        assertThat(tenantRow(tenant)).containsEntry("status", status);
        assertThat(schemaExists(tenant)).isTrue();
        assertThat(outboxFor(tenant)).isEmpty();
        assertThat(purgeEvents.countFor(tenant)).isZero();
    }

    private static void holdTenantRow(Connection connection, TenantId tenant) throws Exception {
        connection.setAutoCommit(false);
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT id FROM global.tenant WHERE id = ?::uuid FOR UPDATE")) {
            lock.setString(1, tenant.value());
            lock.executeQuery().close();
        }
    }

    /** One access session with its reason, one allowed request under it and one denied request without reason. */
    private static void auditedAccess(UUID kuadorUser, TenantId tenant) {
        UUID session = UUID.randomUUID();
        TenantDatabase.admin().sql("""
                        INSERT INTO global.access_session (id, platform_user_id, tenant_id, reason, started_at,
                            expires_at)
                        VALUES (:id, :user, :tenant::uuid, :reason, now(), now() + interval '30 minutes')
                        """)
                .param("id", session).param("user", kuadorUser).param("tenant", tenant.value())
                .param("reason", ACCESS_REASON)
                .update();
        TenantDatabase.admin().sql("""
                        INSERT INTO global.access_audit_event (id, session_id, platform_user_id, tenant_id, reason,
                            http_method, route_template, outcome, occurred_at)
                        VALUES (gen_random_uuid(), :session, :user, :tenant::uuid, :reason, 'GET',
                                '/api/v1/staff/bookings', 'ALLOWED', now()),
                               (gen_random_uuid(), NULL, :user, :tenant::uuid, NULL, 'GET',
                                '/api/v1/staff/bookings', 'DENIED', now())
                        """)
                .param("session", session).param("user", kuadorUser).param("tenant", tenant.value())
                .param("reason", ACCESS_REASON)
                .update();
    }

    private static void forcePurgeDate(TenantId tenant, Instant purgeDueAt) {
        TenantDatabase.admin().sql("UPDATE global.tenant SET purge_due_at = :at WHERE id = :id::uuid")
                .param("at", purgeDueAt.atOffset(ZoneOffset.UTC))
                .param("id", tenant.value())
                .update();
    }

    private static Map<String, Object> tenantRow(TenantId tenant) {
        return TenantDatabase.admin().sql("""
                        SELECT status, display_name, purged_at, owner_email FROM global.tenant WHERE id = :id::uuid
                        """)
                .param("id", tenant.value())
                .query().singleRow();
    }

    private static Map<String, Object> domainRow(String subdomain) {
        return TenantDatabase.admin().sql(
                        "SELECT is_current, retired_at FROM global.tenant_domain WHERE subdomain = :subdomain")
                .param("subdomain", subdomain)
                .query().singleRow();
    }

    private static boolean schemaExists(TenantId tenant) {
        return TenantDatabase.admin().sql(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = :schema)")
                .param("schema", TestTenants.schemaOf(tenant))
                .query(Boolean.class).single();
    }

    private static List<Map<String, Object>> outboxFor(TenantId tenant) {
        return TenantDatabase.admin().sql("""
                        SELECT type, recipient, payload ->> 'displayName' AS display_name
                        FROM global.notification_outbox WHERE payload ->> 'tenantId' = :tenant
                        """)
                .param("tenant", tenant.value())
                .query().listOfRows();
    }

    private static List<String> sessionReasonsOf(TenantId tenant) {
        return TenantDatabase.admin().sql("SELECT reason FROM global.access_session WHERE tenant_id = :id::uuid")
                .param("id", tenant.value())
                .query(String.class).list();
    }

    private static List<String> eventReasonsOf(TenantId tenant) {
        return TenantDatabase.admin().sql("SELECT reason FROM global.access_audit_event WHERE tenant_id = :id::uuid")
                .param("id", tenant.value())
                .query((row, number) -> row.getString("reason")).list();
    }

    /** Rows with a reason whose redaction was not recorded. */
    private static long unredactedRowsOf(TenantId tenant) {
        return TenantDatabase.admin().sql("""
                        SELECT (SELECT count(*) FROM global.access_session
                                WHERE tenant_id = :id::uuid AND reason_redacted_at IS NULL)
                             + (SELECT count(*) FROM global.access_audit_event
                                WHERE tenant_id = :id::uuid AND reason IS NOT NULL AND reason_redacted_at IS NULL)
                        """)
                .param("id", tenant.value())
                .query(Long.class).single();
    }

    /** Counts the {@link TenantPurged} events per complex and can make their consumers fail for one complex. */
    static final class PurgeEvents {

        private final Map<TenantId, AtomicInteger> published = new ConcurrentHashMap<>();
        private final Set<TenantId> failing = ConcurrentHashMap.newKeySet();

        @EventListener
        void on(TenantPurged event) {
            if (failing.contains(event.tenantId())) {
                throw new IllegalStateException("Consumer failure injected by the test");
            }
            published.computeIfAbsent(event.tenantId(), id -> new AtomicInteger()).incrementAndGet();
        }

        void failFor(TenantId tenant) {
            failing.add(tenant);
        }

        void stopFailing() {
            failing.clear();
        }

        int countFor(TenantId tenant) {
            AtomicInteger count = published.get(tenant);
            return count == null ? 0 : count.get();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PurgeEventsConfiguration {

        @Bean
        PurgeEvents purgeEvents() {
            return new PurgeEvents();
        }
    }
}
