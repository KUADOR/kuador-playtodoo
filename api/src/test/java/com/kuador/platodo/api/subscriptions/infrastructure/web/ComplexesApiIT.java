package com.kuador.platodo.api.subscriptions.infrastructure.web;

import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.COMPLEXES;
import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.json;
import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.uniqueSubdomain;
import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

import tools.jackson.databind.JsonNode;

/**
 * Tag «Complexes» of {@code contracts/platform-backoffice.openapi.yaml} over HTTP, with the real token filter,
 * guard, Tenancy provisioning and PostgreSQL (ADR-014, ADR-018; PLATODO-27 CA1, CA2 and CA19). Complexes are global
 * and shared by every test of the JVM, so each test uses subdomains and names of its own.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class ComplexesApiIT {

    private static final String COMPLEX_CREATE = "COMPLEX_CREATE";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private TenantMigrations migrations;

    private ComplexRequests api;
    private String superAdminToken;
    private String planId;
    private String planName;

    @BeforeEach
    void setUp() throws Exception {
        api = new ComplexRequests(mvc);
        ComplexRequests.allowTestPlanOptions();
        superAdminToken = api.superAdminToken();
        planName = "Premium " + UUID.randomUUID();
        planId = api.publishedPlan(superAdminToken, planName);
    }

    @AfterEach
    void tearDown() {
        ComplexRequests.restorePlanOptions();
        setOperatorCapability(COMPLEX_CREATE, false);
    }

    /** CA1: the complex gets its schema, answers on its subdomain and its subscription starts ACTIVE. */
    @Test
    void aSuperAdminRegistersAnActiveComplexReachableByItsSubdomain() throws Exception {
        String subdomain = uniqueSubdomain("el-sol");
        String ownerEmail = TestAccounts.uniqueEmail("owner");

        JsonNode registered = json(api.register(superAdminToken, "Canchas El Sol", subdomain.toUpperCase(),
                        "  " + ownerEmail.toUpperCase() + " ", planId)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(COMPLEXES + "/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.displayName").value("Canchas El Sol"))
                .andExpect(jsonPath("$.subdomain").value(subdomain))
                .andExpect(jsonPath("$.ownerEmail").value(ownerEmail))
                .andExpect(jsonPath("$.tenantStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.suspendedAt").value(nullValue()))
                .andExpect(jsonPath("$.retentionDays").value(nullValue()))
                .andExpect(jsonPath("$.purgeDueAt").value(nullValue()))
                .andExpect(jsonPath("$.purgedAt").value(nullValue()))
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subscription.statusChangedBy.displayName")
                        .value(TestAccounts.PLATFORM_USER_NAME))
                .andExpect(jsonPath("$.subscription.plan.planId").value(planId))
                .andExpect(jsonPath("$.subscription.plan.name").value(planName))
                .andExpect(jsonPath("$.subscription.plan.price").value("120.00"))
                .andExpect(jsonPath("$.subscription.plan.currency").value("USD"))
                .andExpect(jsonPath("$.subscription.plan.periodicity").value("MONTHLY"))
                .andExpect(jsonPath("$.subscription.plan.courtLimit").value(10))
                .andExpect(jsonPath("$.subscription.plan.userLimit").value(25))
                .andExpect(jsonPath("$.subscription.plan.customFields").isMap())
                .andExpect(jsonPath("$.allowedTransitions").value(
                        containsInAnyOrder("EXPIRING", "SUSPENDED", "CANCELLED")))
                .andExpect(jsonPath("$.version").value(1)));
        String complexId = registered.get("id").asString();

        assertThat(json(api.detail(superAdminToken, complexId).andExpect(status().isOk()))).isEqualTo(registered);
        api.tenantState(subdomain).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Canchas El Sol"));
        api.availability(subdomain).andExpect(jsonPath("$.reason").value("TAKEN"));
        assertThat(ownerEmailsIn(new TenantId(complexId))).containsExactly(ownerEmail);
    }

    /** CA19: newest first, with the plan, both statuses and the latest payment; one batch per page. */
    @Test
    void theListShowsPlanStatusAndLastPayment() throws Exception {
        String olderId = registeredComplex(uniqueSubdomain("older"));
        String newerId = registeredComplex(uniqueSubdomain("newer"));
        recordPayment(olderId, "80.5", LocalDate.parse("2026-11-01"));
        recordPayment(olderId, "120.00", LocalDate.parse("2026-12-01"));
        recordPayment(olderId, "99.00", LocalDate.parse("2026-10-01"));

        JsonNode page = json(api.list(superAdminToken, 0, 100)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100)));
        List<String> ids = new ArrayList<>();
        page.get("items").forEach(item -> ids.add(item.get("id").asString()));
        JsonNode older = itemOf(page, olderId);
        JsonNode newer = itemOf(page, newerId);

        assertThat(ids).containsSubsequence(newerId, olderId);
        assertThat(page.get("totalItems").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(older.get("plan").get("id").asString()).isEqualTo(planId);
        assertThat(older.get("plan").get("name").asString()).isEqualTo(planName);
        assertThat(older.get("subscriptionStatus").asString()).isEqualTo("ACTIVE");
        assertThat(older.get("tenantStatus").asString()).isEqualTo("ACTIVE");
        assertThat(older.get("purgeDueAt").isNull()).isTrue();
        assertThat(older.get("lastPayment").get("amount").asString()).isEqualTo("120.00");
        assertThat(older.get("lastPayment").get("currency").asString()).isEqualTo("USD");
        assertThat(older.get("lastPayment").get("paidOn").asString()).isEqualTo("2026-12-01");
        assertThat(newer.get("lastPayment").isNull()).isTrue();

        api.list(superAdminToken, 0, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.size").value(1));
    }

    /** CA2: a used subdomain, in any case, is rejected and nothing is recorded. */
    @Test
    void aTakenSubdomainIsRejectedAndNothingIsCreated() throws Exception {
        String subdomain = uniqueSubdomain("taken");
        registeredComplex(subdomain);
        String secondName = "Second " + UUID.randomUUID();

        expectProblem(api.register(superAdminToken, secondName, subdomain.toUpperCase(),
                TestAccounts.uniqueEmail("owner"), planId), 409, "SUBDOMAIN_TAKEN");

        assertThat(tenantsNamed(secondName)).isZero();
    }

    /** CA2: a reserved or malformed subdomain is rejected and nothing is recorded. */
    @Test
    void aReservedOrInvalidSubdomainIsRejectedAndNothingIsCreated() throws Exception {
        String name = "Rejected " + UUID.randomUUID();

        expectProblem(api.register(superAdminToken, name, "Admin", TestAccounts.uniqueEmail("owner"), planId),
                422, "SUBDOMAIN_RESERVED");
        expectProblem(api.register(superAdminToken, name, "a--b", TestAccounts.uniqueEmail("owner"), planId),
                422, "SUBDOMAIN_INVALID");

        assertThat(tenantsNamed(name)).isZero();
    }

    /** CA3: only a published plan is assigned; a draft or unknown plan leaves the subdomain free. */
    @Test
    void aDraftOrUnknownPlanIsRejectedAndNothingIsCreated() throws Exception {
        String subdomain = uniqueSubdomain("no-plan");
        String name = "No plan " + UUID.randomUUID();
        String draftId = api.draftPlan(superAdminToken, "Draft " + UUID.randomUUID());

        for (String plan : List.of(draftId, UUID.randomUUID().toString())) {
            expectProblem(api.register(superAdminToken, name, subdomain, TestAccounts.uniqueEmail("owner"), plan),
                    422, "PLAN_NOT_PUBLISHED");
        }

        assertThat(tenantsNamed(name)).isZero();
        api.availability(subdomain).andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void invalidFieldsAreReportedTogetherAndMalformedRequestsAreInvalid() throws Exception {
        String subdomain = uniqueSubdomain("invalid");

        expectProblem(api.register(superAdminToken, " ", subdomain, "not-an-email", planId), 422,
                "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[*].field").value(containsInAnyOrder("displayName", "ownerEmail")))
                .andExpect(jsonPath("$.fieldErrors[*].code").value(containsInAnyOrder("REQUIRED", "INVALID_FORMAT")));
        expectProblem(api.as(superAdminToken, post(COMPLEXES), """
                {"displayName": "x", "subdomain": "%s", "ownerEmail": "owner@example.com"}
                """.formatted(subdomain)), 400, "INVALID_REQUEST");
        expectProblem(api.as(superAdminToken, post(COMPLEXES), "{\"planId\": \"not-a-uuid\"}"), 400,
                "INVALID_REQUEST");
        expectProblem(api.list(superAdminToken, 0, 101), 400, "INVALID_REQUEST");
        expectProblem(api.detail(superAdminToken, "not-a-uuid"), 400, "INVALID_REQUEST");
        api.availability(subdomain).andExpect(jsonPath("$.available").value(true));
    }

    /** CA2 under concurrency: two registrations racing for one subdomain record exactly one complex. */
    @Test
    void concurrentRegistrationsWithTheSameSubdomainCreateOneComplex() throws Exception {
        String subdomain = uniqueSubdomain("race");
        String name = "Race " + UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> register = () -> {
            start.await();
            return api.register(superAdminToken, name, subdomain, TestAccounts.uniqueEmail("owner"), planId)
                    .andReturn().getResponse().getStatus();
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(register);
            Future<Integer> second = executor.submit(register);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }

        assertThat(tenantsNamed(name)).isOne();
        assertThat(subscriptionsOfTenantsNamed(name)).isOne();
    }

    /**
     * Isolation (exception E1 of the PLATODO-27 design): provisioning a new complex creates only its own schema and
     * owner, and leaves the data and migrations of an existing complex untouched.
     */
    @Test
    void registeringAComplexDoesNotTouchTheSchemaOfAnother() throws Exception {
        TenantId existing = TestTenants.provision(migrations, TestTenants.A);
        String existingStaff = TestAccounts.uniqueEmail("existing-staff");
        TestAccounts.staff(existing, existingStaff, "STAFF");
        List<String> staffBefore = staffEmailsIn(existing);
        long migrationsBefore = appliedMigrationsIn(existing);
        String ownerEmail = TestAccounts.uniqueEmail("isolated-owner");

        String complexId = json(api.register(superAdminToken, "Isolated", uniqueSubdomain("isolated"), ownerEmail,
                planId).andExpect(status().isCreated())).get("id").asString();

        assertThat(staffEmailsIn(existing)).isEqualTo(staffBefore).doesNotContain(ownerEmail);
        assertThat(appliedMigrationsIn(existing)).isEqualTo(migrationsBefore);
        assertThat(staffEmailsIn(new TenantId(complexId))).containsExactly(ownerEmail);
    }

    /** CA15: operators read the list and the detail, and register only with COMPLEX_CREATE enabled. */
    @Test
    void operatorsRegisterOnlyWithTheCapability() throws Exception {
        String operatorToken = api.operatorToken();
        String complexId = registeredComplex(uniqueSubdomain("seen"));

        api.list(operatorToken, 0, 20).andExpect(status().isOk());
        api.detail(operatorToken, complexId).andExpect(status().isOk());
        expectProblem(api.register(operatorToken, "By operator", uniqueSubdomain("operator"),
                TestAccounts.uniqueEmail("owner"), planId), 403, "CAPABILITY_REVOKED");
        expectProblem(api.retry(operatorToken, complexId), 403, "CAPABILITY_REVOKED");

        setOperatorCapability(COMPLEX_CREATE, true);

        api.register(operatorToken, "By operator", uniqueSubdomain("operator"), TestAccounts.uniqueEmail("owner"),
                planId).andExpect(status().isCreated());
    }

    /** CA18: players and anonymous callers never reach the complexes; neither does a tenant host. */
    @Test
    void playersAnonymousCallersAndTenantHostsDoNotReachComplexes() throws Exception {
        String playerToken = api.playerToken();
        String complexId = registeredComplex(uniqueSubdomain("guarded"));

        expectProblem(api.list(playerToken, 0, 20), 403, "FORBIDDEN");
        expectProblem(api.detail(playerToken, complexId), 403, "FORBIDDEN");
        expectProblem(api.register(playerToken, "By player", uniqueSubdomain("player"),
                TestAccounts.uniqueEmail("owner"), planId), 403, "FORBIDDEN");
        expectProblem(mvc.perform(get(COMPLEXES).with(onHost(TestTenants.GLOBAL_HOST))), 401, "UNAUTHENTICATED");
        mvc.perform(get(COMPLEXES).with(onHost(TestTenants.hostOf(TestTenants.A)))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownComplexesAreNotFoundAndOnlyFailedProvisioningsAreRetried() throws Exception {
        String complexId = registeredComplex(uniqueSubdomain("no-retry"));

        expectProblem(api.detail(superAdminToken, UUID.randomUUID().toString()), 404, "COMPLEX_NOT_FOUND");
        expectProblem(api.retry(superAdminToken, UUID.randomUUID().toString()), 404, "COMPLEX_NOT_FOUND");
        expectProblem(api.retry(superAdminToken, complexId), 409, "NOT_PROVISIONING_FAILED");
    }

    private String registeredComplex(String subdomain) throws Exception {
        return json(api.register(superAdminToken, "Complex " + subdomain, subdomain,
                TestAccounts.uniqueEmail("owner"), planId).andExpect(status().isCreated())).get("id").asString();
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
    }

    private static JsonNode itemOf(JsonNode page, String complexId) {
        for (JsonNode item : page.get("items")) {
            if (item.get("id").asString().equals(complexId)) {
                return item;
            }
        }
        throw new AssertionError("Complex " + complexId + " is not in the page");
    }

    /** A payment as PLATODO-85 will record it; recording payments is not part of this task. */
    private static void recordPayment(String complexId, String amount, LocalDate paidOn) {
        TenantDatabase.admin().sql("""
                        INSERT INTO global.subscription_payment
                            (id, tenant_id, amount, currency, paid_on, method, recorded_by, recorded_at)
                        SELECT gen_random_uuid(), s.tenant_id, CAST(:amount AS numeric), 'USD', :paidOn, 'TRANSFER',
                               s.status_changed_by, now()
                        FROM global.subscription s WHERE s.tenant_id = CAST(:tenantId AS uuid)
                        """)
                .param("amount", amount)
                .param("paidOn", paidOn)
                .param("tenantId", complexId)
                .update();
    }

    private static long tenantsNamed(String displayName) {
        return TenantDatabase.admin().sql("SELECT count(*) FROM global.tenant WHERE display_name = :name")
                .param("name", displayName).query(Long.class).single();
    }

    private static long subscriptionsOfTenantsNamed(String displayName) {
        return TenantDatabase.admin().sql("""
                        SELECT count(*) FROM global.subscription s JOIN global.tenant t ON t.id = s.tenant_id
                        WHERE t.display_name = :name
                        """)
                .param("name", displayName).query(Long.class).single();
    }

    private static List<String> staffEmailsIn(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT email FROM \"%s\".staff_user ORDER BY email".formatted(TestTenants.schemaOf(tenant)))
                .query(String.class).list();
    }

    private static List<String> ownerEmailsIn(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT email FROM \"%s\".staff_user WHERE role = 'OWNER'".formatted(TestTenants.schemaOf(tenant)))
                .query(String.class).list();
    }

    private static long appliedMigrationsIn(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT count(*) FROM \"%s\".flyway_schema_history".formatted(TestTenants.schemaOf(tenant)))
                .query(Long.class).single();
    }

    private static void setOperatorCapability(String capability, boolean enabled) {
        TenantDatabase.admin().sql("""
                        UPDATE global.operator_capability SET enabled = :enabled, version = version + 1
                        WHERE capability_key = :capability AND enabled <> :enabled
                        """)
                .param("enabled", enabled).param("capability", capability).update();
    }
}
