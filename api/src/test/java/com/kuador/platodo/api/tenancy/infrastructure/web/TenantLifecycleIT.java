package com.kuador.platodo.api.tenancy.infrastructure.web;

import static com.kuador.platodo.api.tenancy.infrastructure.web.TenancyRequests.json;
import static com.kuador.platodo.api.tenancy.infrastructure.web.TenancyRequests.uniqueSubdomain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.event.TenantActivated;
import com.kuador.platodo.api.tenancy.domain.event.TenantArchived;
import com.kuador.platodo.api.tenancy.domain.event.TenantSuspended;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

import tools.jackson.databind.JsonNode;

/**
 * Registration and lifecycle of a complex by Kuador (ADR-013), over HTTP with the real wiring and PostgreSQL. Numbers
 * refer to the criteria of PLATODO-26.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
@RecordApplicationEvents
class TenantLifecycleIT {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final int DEFAULT_RETENTION_DAYS = 45;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ApplicationEvents events;
    @Autowired
    private TenantMigrations migrations;

    private TenancyRequests api;
    private String ownerToken;

    @BeforeEach
    void setUp() throws Exception {
        api = new TenancyRequests(mvc);
        ownerToken = api.platformOwnerToken();
    }

    /** Criterion 7: a new complex gets its own schema, migrated, with its owner; it resolves on its subdomain. */
    @Test
    void registeringAComplexProvisionsItsSchemaAndOwner() throws Exception {
        String subdomain = uniqueSubdomain("new");
        String ownerEmail = TestAccounts.uniqueEmail("Owner");

        JsonNode tenant = json(api.register(ownerToken, "Canchas El Sol", subdomain.toUpperCase(), ownerEmail)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subdomain").value(subdomain))
                .andExpect(jsonPath("$.displayName").value("Canchas El Sol"))
                .andExpect(jsonPath("$.activatedAt").isNotEmpty())
                .andExpect(jsonPath("$.archivedAt").isEmpty())
                .andExpect(jsonPath("$.purgeDueAt").isEmpty()));
        TenantId id = new TenantId(tenant.get("id").asString());

        assertThat(schemaExists(id)).isTrue();
        assertThat(ownersIn(id)).containsExactly(ownerEmail.toLowerCase());
        api.tenantState(subdomain).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Canchas El Sol"));
        assertThat(events.stream(TenantActivated.class))
                .containsExactly(new TenantActivated(id, subdomain, Instant.parse(tenant.get("activatedAt").asString())));
    }

    @Test
    void theRegistrationAnswersTheLocationOfTheTenant() throws Exception {
        ResultActions created = api.register(ownerToken, "La Esquina", uniqueSubdomain("loc"),
                TestAccounts.uniqueEmail("owner")).andExpect(status().isCreated());

        String id = json(created).get("id").asString();
        created.andExpect(header().string("Location", TenancyRequests.PLATFORM_TENANTS + "/" + id));
    }

    /** Criterion 7: the data of the new complex is not visible from another subdomain, nor its staff accepted. */
    @Test
    void theNewComplexIsIsolatedFromTheOthers() throws Exception {
        String subdomain = uniqueSubdomain("iso");
        TenantId id = api.registered(ownerToken, subdomain);
        TestTenants.provision(migrations, TestTenants.A);
        String staffEmail = TestAccounts.uniqueEmail("staff-new");
        TestAccounts.staff(id, staffEmail, "ADMIN");

        api.staffLogin(subdomain, staffEmail).andExpect(status().isOk());
        api.staffLogin(TestTenants.subdomainOf(TestTenants.A), staffEmail)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        assertThat(ownersIn(TestTenants.A)).doesNotContainAnyElementsOf(ownersIn(id));
    }

    @Test
    void onlyThePlatformOwnerRegistersComplexes() throws Exception {
        String subdomain = uniqueSubdomain("auth");

        expectProblem(api.register(null, "No token", subdomain, TestAccounts.uniqueEmail("o")), 401,
                "UNAUTHENTICATED");
        expectProblem(api.register(api.playerToken(), "Player", subdomain, TestAccounts.uniqueEmail("o")), 403,
                "FORBIDDEN");
        api.availability(subdomain).andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void theSubdomainRulesApplyToTheRegistration() throws Exception {
        expectProblem(api.register(ownerToken, "Reserved", "admin", TestAccounts.uniqueEmail("o")), 422,
                "SUBDOMAIN_RESERVED");
        expectProblem(api.register(ownerToken, "Reserved", "API", TestAccounts.uniqueEmail("o")), 422,
                "SUBDOMAIN_RESERVED");
        expectProblem(api.register(ownerToken, "Invalid", "a--b", TestAccounts.uniqueEmail("o")), 422,
                "SUBDOMAIN_INVALID");
    }

    @Test
    void aMalformedRegistrationIsAnInvalidRequest() throws Exception {
        expectProblem(api.register(ownerToken, " ", uniqueSubdomain("bad"), TestAccounts.uniqueEmail("o")), 400,
                "INVALID_REQUEST");
        expectProblem(api.register(ownerToken, "No e-mail", uniqueSubdomain("bad"), "not-an-email"), 400,
                "INVALID_REQUEST");
        expectProblem(api.register(ownerToken, "Long", "a".repeat(256), TestAccounts.uniqueEmail("o")), 400,
                "INVALID_REQUEST");
    }

    @Test
    void aTakenSubdomainIsAConflictAndRecordsNothing() throws Exception {
        String subdomain = uniqueSubdomain("taken");
        api.registered(ownerToken, subdomain);
        String displayName = "Second " + subdomain;

        expectProblem(api.register(ownerToken, displayName, subdomain, TestAccounts.uniqueEmail("o")), 409,
                "SUBDOMAIN_TAKEN");

        assertThat(tenantsNamed(displayName)).isZero();
    }

    /** Criterion 21: two registrations racing for one subdomain: one gets it, the other one is a 409. */
    @Test
    void concurrentRegistrationsWithTheSameSubdomainHaveOneWinner() throws Exception {
        String subdomain = uniqueSubdomain("race");
        String displayName = "Race " + subdomain;
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<Integer> registration = () -> {
            start.await();
            return api.register(ownerToken, displayName, subdomain, TestAccounts.uniqueEmail("o"))
                    .andReturn().getResponse().getStatus();
        };

        List<Integer> statuses = runTogether(registration, registration);

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(tenantsNamed(displayName)).isOne();
        assertThat(currentOwnersOf(subdomain)).isOne();
    }

    @Test
    void aSuspendedComplexIsUnavailableUntilItIsActivated() throws Exception {
        String subdomain = uniqueSubdomain("susp");
        TenantId id = api.registered(ownerToken, subdomain);

        JsonNode suspended = json(api.lifecycle(ownerToken, id, "suspend")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.suspendedAt").isNotEmpty()));
        expectProblem(api.tenantState(subdomain), 403, "TENANT_UNAVAILABLE");

        api.lifecycle(ownerToken, id, "activate")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.suspendedAt").isEmpty());
        api.tenantState(subdomain).andExpect(status().isOk());
        assertThat(events.stream(TenantSuspended.class))
                .containsExactly(new TenantSuspended(id, Instant.parse(suspended.get("suspendedAt").asString())));
        assertThat(events.stream(TenantActivated.class)).hasSize(2);
    }

    /** Invalid transitions answer 409 INVALID_TRANSITION and change nothing. */
    @Test
    void transitionsTheStateDoesNotAllowAreConflicts() throws Exception {
        TenantId id = api.registered(ownerToken, uniqueSubdomain("inv"));

        expectProblem(api.lifecycle(ownerToken, id, "activate"), 409, "INVALID_TRANSITION");
        expectProblem(api.lifecycle(ownerToken, id, "reactivate"), 409, "INVALID_TRANSITION");
        expectProblem(api.changeRetention(ownerToken, id, "{\"days\": 60}"), 409, "INVALID_TRANSITION");
        api.lifecycle(ownerToken, id, "suspend").andExpect(status().isOk());
        expectProblem(api.lifecycle(ownerToken, id, "suspend"), 409, "INVALID_TRANSITION");
        api.lifecycle(ownerToken, id, "archive").andExpect(status().isOk());
        expectProblem(api.lifecycle(ownerToken, id, "archive"), 409, "INVALID_TRANSITION");
        expectProblem(api.lifecycle(ownerToken, id, "suspend"), 409, "INVALID_TRANSITION");
        expectProblem(api.lifecycle(ownerToken, id, "activate"), 409, "INVALID_TRANSITION");

        assertThat(statusOf(id)).isEqualTo("ARCHIVED");
    }

    /** Criterion 12: an archived complex is not accessible; reactivated in time, it recovers all its data. */
    @Test
    void anArchivedComplexReactivatedInTimeRecoversItsData() throws Exception {
        String subdomain = uniqueSubdomain("arch");
        TenantId id = api.registered(ownerToken, subdomain);
        String staffEmail = TestAccounts.uniqueEmail("staff");
        TestAccounts.staff(id, staffEmail, "ADMIN");

        JsonNode archived = json(api.lifecycle(ownerToken, id, "archive")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.retentionDays").value(DEFAULT_RETENTION_DAYS)));
        Instant archivedAt = Instant.parse(archived.get("archivedAt").asString());
        Instant purgeDueAt = Instant.parse(archived.get("purgeDueAt").asString());
        assertThat(purgeDueAt).isEqualTo(archivedAt.plus(Duration.ofDays(DEFAULT_RETENTION_DAYS)));
        expectProblem(api.tenantState(subdomain), 403, "TENANT_UNAVAILABLE");
        expectProblem(api.staffLogin(subdomain, staffEmail), 403, "TENANT_UNAVAILABLE");

        api.lifecycle(ownerToken, id, "reactivate")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.archivedAt").isEmpty())
                .andExpect(jsonPath("$.retentionDays").isEmpty())
                .andExpect(jsonPath("$.purgeDueAt").isEmpty());

        api.staffLogin(subdomain, staffEmail).andExpect(status().isOk());
        assertThat(staffIn(id)).contains(staffEmail);
        assertThat(events.stream(TenantArchived.class))
                .containsExactly(new TenantArchived(id, purgeDueAt, archivedAt));
    }

    @Test
    void aComplexPastItsPurgeDateCannotBeReactivated() throws Exception {
        TenantId id = api.registered(ownerToken, uniqueSubdomain("late"));
        api.lifecycle(ownerToken, id, "archive").andExpect(status().isOk());
        movePurgeDate(id, Instant.now().minusSeconds(1));

        expectProblem(api.lifecycle(ownerToken, id, "reactivate"), 409, "RETENTION_EXPIRED");

        assertThat(statusOf(id)).isEqualTo("ARCHIVED");
    }

    @Test
    void kuadorChangesTheRetentionOfAnArchivedComplex() throws Exception {
        TenantId id = api.registered(ownerToken, uniqueSubdomain("ret"));
        Instant archivedAt = Instant.parse(json(api.lifecycle(ownerToken, id, "archive")).get("archivedAt").asString());

        api.changeRetention(ownerToken, id, "{\"days\": 60}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionDays").value(60))
                .andExpect(jsonPath("$.purgeDueAt").value(archivedAt.plus(Duration.ofDays(60)).toString()));
        expectProblem(api.changeRetention(ownerToken, id, "{\"days\": 0}"), 400, "INVALID_REQUEST");
        expectProblem(api.changeRetention(ownerToken, id, "{}"), 400, "INVALID_REQUEST");

        moveArchiveDate(id, Instant.now().minus(Duration.ofDays(30)));
        expectProblem(api.changeRetention(ownerToken, id, "{\"days\": 10}"), 422, "RETENTION_INVALID");
        assertThat(retentionOf(id)).isEqualTo(60);
    }

    @Test
    void anUnknownTenantIdIsNotFound() throws Exception {
        expectProblem(api.lifecycle(ownerToken, new TenantId(UUID.randomUUID().toString()), "suspend"), 404,
                "TENANT_NOT_FOUND");
        expectProblem(api.lifecycle(ownerToken, new TenantId("not-a-uuid"), "suspend"), 404, "TENANT_NOT_FOUND");
    }

    @Test
    void lifecycleOperationsNeedThePlatformOwner() throws Exception {
        TenantId id = api.registered(ownerToken, uniqueSubdomain("perm"));

        expectProblem(api.lifecycle(null, id, "suspend"), 401, "UNAUTHENTICATED");
        expectProblem(api.lifecycle(api.playerToken(), id, "archive"), 403, "FORBIDDEN");

        assertThat(statusOf(id)).isEqualTo("ACTIVE");
    }

    /** A failed provisioning stays PROVISIONING; activate retries it once the owner account exists. */
    @Test
    void activatingRetriesAFailedProvisioning() throws Exception {
        TenantId id = new TenantId(UUID.randomUUID().toString());
        TestTenants.register(id, "PROVISIONING");

        expectProblem(api.lifecycle(ownerToken, id, "activate"), 409, "INVALID_TRANSITION");
        assertThat(schemaExists(id)).isTrue();
        assertThat(statusOf(id)).isEqualTo("PROVISIONING");

        TestAccounts.staff(id, TestAccounts.uniqueEmail("owner"), "OWNER");
        api.lifecycle(ownerToken, id, "activate")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        api.tenantState(TestTenants.subdomainOf(id)).andExpect(status().isOk());
    }

    private static List<Integer> runTogether(Callable<Integer> first, Callable<Integer> second) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = new ArrayList<>();
            results.add(executor.submit(first));
            results.add(executor.submit(second));
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            return statuses;
        }
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(code));
    }

    private static boolean schemaExists(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = :schema)")
                .param("schema", TestTenants.schemaOf(tenant))
                .query(Boolean.class)
                .single();
    }

    private static List<String> ownersIn(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT email FROM \"%s\".staff_user WHERE role = 'OWNER'".formatted(TestTenants.schemaOf(tenant)))
                .query(String.class)
                .list();
    }

    private static List<String> staffIn(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT email FROM \"%s\".staff_user".formatted(TestTenants.schemaOf(tenant)))
                .query(String.class)
                .list();
    }

    private static long tenantsNamed(String displayName) {
        return TenantDatabase.admin().sql("SELECT count(*) FROM global.tenant WHERE display_name = :name")
                .param("name", displayName)
                .query(Long.class)
                .single();
    }

    private static long currentOwnersOf(String subdomain) {
        return TenantDatabase.admin()
                .sql("SELECT count(*) FROM global.tenant_domain WHERE subdomain = :subdomain AND is_current")
                .param("subdomain", subdomain)
                .query(Long.class)
                .single();
    }

    private static String statusOf(TenantId tenant) {
        return TenantDatabase.admin().sql("SELECT status FROM global.tenant WHERE id = :id::uuid")
                .param("id", tenant.value())
                .query(String.class)
                .single();
    }

    private static Integer retentionOf(TenantId tenant) {
        return TenantDatabase.admin().sql("SELECT retention_days FROM global.tenant WHERE id = :id::uuid")
                .param("id", tenant.value())
                .query(Integer.class)
                .single();
    }

    private static void movePurgeDate(TenantId tenant, Instant purgeDueAt) {
        TenantDatabase.admin().sql("UPDATE global.tenant SET purge_due_at = :at WHERE id = :id::uuid")
                .param("at", Timestamp.from(purgeDueAt))
                .param("id", tenant.value())
                .update();
    }

    private static void moveArchiveDate(TenantId tenant, Instant archivedAt) {
        TenantDatabase.admin().sql("UPDATE global.tenant SET archived_at = :at WHERE id = :id::uuid")
                .param("at", Timestamp.from(archivedAt))
                .param("id", tenant.value())
                .update();
    }
}
