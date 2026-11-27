package com.kuador.platodo.api.subscriptions.infrastructure.web;

import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.COMPLEXES;
import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.json;
import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.uniqueSubdomain;
import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnPlayerApiEnabled;
import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnStaffApiEnabled;
import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.complexes.application.port.out.ComplexCatalog;
import com.kuador.platodo.api.complexes.domain.CatalogEntry;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;
import com.kuador.platodo.api.tenancy.domain.event.TenantStatusChanged;

import tools.jackson.databind.JsonNode;

/**
 * Effect of the subscription status on the tenant and its retention (ADR-014, ADR-019; PLATODO-27 CA8 and CA9), over
 * HTTP with the real tenant filter, token filter, guard, booking endpoints and PostgreSQL. Complexes are global and
 * shared by every test of the JVM, so each test registers complexes of its own.
 */
@SpringBootTest(properties = {
        ConditionalOnPlayerApiEnabled.PROPERTY + "=true",
        ConditionalOnStaffApiEnabled.PROPERTY + "=true"})
@AutoConfigureMockMvc
@RecordApplicationEvents
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class SubscriptionLifecycleEffectsIT {

    private static final long REGISTERED_VERSION = 1;
    private static final int DEFAULT_RETENTION_DAYS = 45;
    private static final String STAFF_LOGIN = "/api/v1/auth/staff/login";
    private static final String STAFF_BOOKINGS = "/api/v1/staff/bookings";
    private static final String FUTURE_SLOT = "2030-01-01T20:00:00Z";
    private static final String STATUS_CHANGE = "SUBSCRIPTION_STATUS_CHANGE";
    /** Prefixes of the operations of the global host (contracts/tenancy.openapi.yaml), never served on a tenant host. */
    private static final List<String> GLOBAL_OPERATIONS = List.of(
            "/api/v1/platform/", "/api/v1/auth/players/", "/api/v1/auth/platform/", "/api/v1/subdomains/");
    private static final String SPRING_ERROR_PATH = "/error";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ApplicationEvents events;
    @Autowired
    private StubCourtSlotRules courts;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    private ComplexRequests api;
    private String superAdminToken;
    private String planId;

    @BeforeEach
    void setUp() throws Exception {
        api = new ComplexRequests(mvc);
        ComplexRequests.allowTestPlanOptions();
        superAdminToken = api.superAdminToken();
        planId = api.publishedPlan(superAdminToken, "Effects " + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        ComplexRequests.restorePlanOptions();
        setOperatorCapability(STATUS_CHANGE, false);
        FailingCatalog.FAILING_TENANTS.clear();
    }

    /** CA8: suspending blocks staff and players on every endpoint of app and movil and starts the retention. */
    @Test
    void suspendingBlocksEveryTenantEndpointAndStartsTheRetention() throws Exception {
        Complex complex = registeredComplex("suspend");
        assertThat(catalogVisibilityOf(complex)).isTrue();
        String staffToken = complex.staffToken();
        String playerToken = api.playerToken();

        JsonNode suspended = json(api.changeStatus(superAdminToken, complex.id(), "SUSPENDED", REGISTERED_VERSION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.retentionDays").value(DEFAULT_RETENTION_DAYS))
                .andExpect(jsonPath("$.archivedAt").isEmpty())
                .andExpect(jsonPath("$.allowedTransitions.length()").value(2)));

        Instant suspendedAt = Instant.parse(suspended.get("suspendedAt").asString());
        assertThat(suspendedAt).isEqualTo(Instant.parse(suspended.get("subscription").get("statusSince").asString()));
        assertThat(Instant.parse(suspended.get("purgeDueAt").asString()))
                .isEqualTo(suspendedAt.plus(Duration.ofDays(DEFAULT_RETENTION_DAYS)));
        assertThat(catalogVisibilityOf(complex)).isFalse();
        assertThat(blockedTenantRoutes(complex, staffToken)).containsAll(appAndMovilRoutes());
        assertThat(blockedTenantRoutes(complex, playerToken)).containsAll(appAndMovilRoutes());
        assertThat(statusEventsOf(complex)).singleElement().satisfies(changed -> {
            assertThat(changed.status()).isEqualTo(TenantStatus.SUSPENDED);
            assertThat(changed.purgeDueAt()).isEqualTo(suspendedAt.plus(Duration.ofDays(DEFAULT_RETENTION_DAYS)));
        });
    }

    /** CA8 with answer 1: cancelling an active complex blocks it like a suspension, retention from the cancellation. */
    @Test
    void cancellingAnActiveComplexArchivesItWithTheRetentionFromTheCancellation() throws Exception {
        Complex complex = registeredComplex("cancel");

        JsonNode cancelled = json(api.changeStatus(superAdminToken, complex.id(), "CANCELLED", REGISTERED_VERSION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantStatus").value("ARCHIVED"))
                .andExpect(jsonPath("$.suspendedAt").isEmpty())
                .andExpect(jsonPath("$.retentionDays").value(DEFAULT_RETENTION_DAYS))
                .andExpect(jsonPath("$.allowedTransitions").value(empty())));

        Instant archivedAt = Instant.parse(cancelled.get("archivedAt").asString());
        assertThat(Instant.parse(cancelled.get("purgeDueAt").asString()))
                .isEqualTo(archivedAt.plus(Duration.ofDays(DEFAULT_RETENTION_DAYS)));
        expectProblem(api.tenantState(complex.subdomain()), 403, "TENANT_UNAVAILABLE");
        expectProblem(staffLogin(complex), 403, "TENANT_UNAVAILABLE");
        assertThat(catalogVisibilityOf(complex)).isFalse();
    }

    @Test
    void cancellingASuspendedComplexKeepsThePurgeDateThatWasRunning() throws Exception {
        Complex complex = registeredComplex("cancel-suspended");
        JsonNode suspended = json(api.changeStatus(superAdminToken, complex.id(), "SUSPENDED", REGISTERED_VERSION));

        api.changeStatus(superAdminToken, complex.id(), "CANCELLED", suspended.get("version").asLong())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantStatus").value("ARCHIVED"))
                .andExpect(jsonPath("$.archivedAt").isNotEmpty())
                .andExpect(jsonPath("$.suspendedAt").value(suspended.get("suspendedAt").asString()))
                .andExpect(jsonPath("$.purgeDueAt").value(suspended.get("purgeDueAt").asString()))
                .andExpect(jsonPath("$.retentionDays").value(DEFAULT_RETENTION_DAYS));
    }

    /** CA9 and ADR-019: reactivated in time, the complex opens again with its bookings exactly as they were. */
    @Test
    void reactivatingBeforeThePurgeDateRestoresAccessWithTheBookingsIntact() throws Exception {
        Complex complex = registeredComplex("reactivate");
        String staffToken = complex.staffToken();
        createBooking(complex, staffToken).andExpect(jsonPath("$.inactive").value(false));
        List<Map<String, Object>> bookingsBefore = bookingRowsOf(complex);
        JsonNode suspended = json(api.changeStatus(superAdminToken, complex.id(), "SUSPENDED", REGISTERED_VERSION));

        api.changeStatus(superAdminToken, complex.id(), "ACTIVE", suspended.get("version").asLong())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tenantStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.suspendedAt").isEmpty())
                .andExpect(jsonPath("$.retentionDays").isEmpty())
                .andExpect(jsonPath("$.purgeDueAt").isEmpty());

        assertThat(bookingRowsOf(complex)).isNotEmpty().isEqualTo(bookingsBefore);
        listBookings(complex, staffToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inactive").value(false));
        api.tenantState(complex.subdomain()).andExpect(status().isOk());
        assertThat(catalogVisibilityOf(complex)).isTrue();
        assertThat(statusEventsOf(complex).stream().map(TenantStatusChanged::status))
                .containsExactly(TenantStatus.SUSPENDED, TenantStatus.ACTIVE);
    }

    /** CA9: from the purge date on, and while purging, a suspended subscription is never reactivated. */
    @Test
    void reactivatingFromThePurgeDateOnIsRetentionExpired() throws Exception {
        Complex late = registeredComplex("late");
        long version = json(api.changeStatus(superAdminToken, late.id(), "SUSPENDED", REGISTERED_VERSION))
                .get("version").asLong();
        moveTenantDate(late, "purge_due_at", Instant.now().minusSeconds(1));

        api.detail(superAdminToken, late.id()).andExpect(jsonPath("$.allowedTransitions").value(empty()));
        expectProblem(api.changeStatus(superAdminToken, late.id(), "ACTIVE", version), 422, "RETENTION_EXPIRED");

        Complex purging = registeredComplex("purging");
        long purgingVersion = json(api.changeStatus(superAdminToken, purging.id(), "SUSPENDED", REGISTERED_VERSION))
                .get("version").asLong();
        setTenantStatus(purging, "PURGING");
        expectProblem(api.changeStatus(superAdminToken, purging.id(), "ACTIVE", purgingVersion), 422,
                "RETENTION_EXPIRED");

        api.detail(superAdminToken, late.id())
                .andExpect(jsonPath("$.subscription.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.tenantStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.version").value(version));
    }

    /** ADR-014: the SUPER_ADMIN recomputes the purge date from the start of the retention. */
    @Test
    void theSuperAdminChangesTheRetentionFromTheDateItStarted() throws Exception {
        Complex complex = registeredComplex("retention");
        JsonNode suspended = json(api.changeStatus(superAdminToken, complex.id(), "SUSPENDED", REGISTERED_VERSION));
        Instant suspendedAt = Instant.parse(suspended.get("suspendedAt").asString());

        JsonNode extended = json(changeRetention(superAdminToken, complex.id(), 60, suspended.get("version").asLong())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.retentionDays").value(60))
                .andExpect(jsonPath("$.purgeDueAt").value(suspendedAt.plus(Duration.ofDays(60)).toString())));
        JsonNode cancelled = json(api.changeStatus(superAdminToken, complex.id(), "CANCELLED",
                extended.get("version").asLong()).andExpect(jsonPath("$.retentionDays").value(60)));
        changeRetention(superAdminToken, complex.id(), 90, cancelled.get("version").asLong())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantStatus").value("ARCHIVED"))
                .andExpect(jsonPath("$.purgeDueAt").value(suspendedAt.plus(Duration.ofDays(90)).toString()));

        Complex cancelledWhileActive = registeredComplex("retention-cancelled");
        JsonNode archived = json(api.changeStatus(superAdminToken, cancelledWhileActive.id(), "CANCELLED",
                REGISTERED_VERSION));
        Instant archivedAt = Instant.parse(archived.get("archivedAt").asString());
        changeRetention(superAdminToken, cancelledWhileActive.id(), 10, archived.get("version").asLong())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purgeDueAt").value(archivedAt.plus(Duration.ofDays(10)).toString()));
    }

    @Test
    void aRetentionChangeOutsideTheRulesChangesNothing() throws Exception {
        Complex active = registeredComplex("retention-active");
        expectProblem(changeRetention(superAdminToken, active.id(), 60, REGISTERED_VERSION), 422, "NOT_IN_RETENTION");

        Complex complex = registeredComplex("retention-rejected");
        long version = json(api.changeStatus(superAdminToken, complex.id(), "SUSPENDED", REGISTERED_VERSION))
                .get("version").asLong();
        moveTenantDate(complex, "suspended_at", Instant.now().minus(Duration.ofDays(30)));

        expectProblem(changeRetention(superAdminToken, complex.id(), 10, version), 422, "RETENTION_DEADLINE_IN_PAST");
        expectProblem(changeRetention(superAdminToken, complex.id(), 0, version), 422, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("retentionDays"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("NOT_POSITIVE"));
        expectProblem(api.as(superAdminToken, put(COMPLEXES + "/" + complex.id() + "/retention"), "{\"version\": 1}"),
                400, "INVALID_REQUEST");
        expectProblem(changeRetention(superAdminToken, complex.id(), 60, version - 1), 409, "STALE_STATE");
        expectProblem(changeRetention(superAdminToken, UUID.randomUUID().toString(), 60, 0), 404,
                "COMPLEX_NOT_FOUND");

        api.detail(superAdminToken, complex.id())
                .andExpect(jsonPath("$.retentionDays").value(DEFAULT_RETENTION_DAYS))
                .andExpect(jsonPath("$.version").value(version));
    }

    /** ADR-017: changing the retention is reserved to the SUPER_ADMIN, whatever the operator has enabled. */
    @Test
    void onlyTheSuperAdminChangesTheRetention() throws Exception {
        Complex complex = registeredComplex("retention-operator");
        long version = json(api.changeStatus(superAdminToken, complex.id(), "SUSPENDED", REGISTERED_VERSION))
                .get("version").asLong();
        setOperatorCapability(STATUS_CHANGE, true);

        expectProblem(changeRetention(api.operatorToken(), complex.id(), 60, version), 403, "FORBIDDEN");
        expectProblem(changeRetention(api.playerToken(), complex.id(), 60, version), 403, "FORBIDDEN");

        api.detail(superAdminToken, complex.id()).andExpect(jsonPath("$.retentionDays").value(DEFAULT_RETENTION_DAYS));
    }

    /**
     * ADR-014: the catalog follows {@link TenantStatusChanged} inside the change transaction, so if it fails after
     * hiding the complex, neither the catalog, the tenant nor the subscription change: they never drift apart.
     */
    @Test
    void aFailingCatalogRollsBackTheCatalogTheTenantAndTheSubscription() throws Exception {
        Complex complex = registeredComplex("catalog-rollback");
        FailingCatalog.FAILING_TENANTS.add(UUID.fromString(complex.id()));

        assertThatThrownBy(() -> api.changeStatus(superAdminToken, complex.id(), "SUSPENDED", REGISTERED_VERSION))
                .rootCause().hasMessage(FailingCatalog.FAILURE);

        assertThat(catalogVisibilityOf(complex)).isTrue();

        api.detail(superAdminToken, complex.id())
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tenantStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.purgeDueAt").isEmpty())
                .andExpect(jsonPath("$.version").value(REGISTERED_VERSION));
        api.tenantState(complex.subdomain()).andExpect(status().isOk());
    }

    /** AGENTS.md isolation: suspending and changing the retention of one complex leaves another one untouched. */
    @Test
    void theEffectOnOneComplexNeverTouchesAnother() throws Exception {
        Complex suspended = registeredComplex("isolated-suspended");
        Complex untouched = registeredComplex("isolated-untouched");
        String untouchedStaff = untouched.staffToken();
        createBooking(untouched, untouchedStaff);
        List<Map<String, Object>> untouchedBookings = bookingRowsOf(untouched);
        JsonNode untouchedBefore = json(api.detail(superAdminToken, untouched.id()));

        long version = json(api.changeStatus(superAdminToken, suspended.id(), "SUSPENDED", REGISTERED_VERSION))
                .get("version").asLong();
        changeRetention(superAdminToken, suspended.id(), 60, version).andExpect(status().isOk());

        assertThat(json(api.detail(superAdminToken, untouched.id()))).isEqualTo(untouchedBefore);
        assertThat(bookingRowsOf(untouched)).isEqualTo(untouchedBookings);
        listBookings(untouched, untouchedStaff)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inactive").value(false));
        expectProblem(api.tenantState(suspended.subdomain()), 403, "TENANT_UNAVAILABLE");
        assertThat(statusEventsOf(untouched)).isEmpty();
        assertThat(catalogVisibilityOf(untouched)).isTrue();
        assertThat(catalogVisibilityOf(suspended)).isFalse();
    }

    /**
     * Every operation of a tenant host that answers {@code 403 TENANT_UNAVAILABLE} on the host of {@code complex},
     * as {@code METHOD path}. Fails if any of them answers anything else, so no endpoint escapes the block.
     */
    private Set<String> blockedTenantRoutes(Complex complex, String token) throws Exception {
        Set<String> blocked = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, ?> mapping : mappings.getHandlerMethods().entrySet()) {
            for (String route : routesOf(mapping.getKey())) {
                if (isGlobalOperation(route)) {
                    continue;
                }
                String[] methodAndPath = route.split(" ", 2);
                String path = methodAndPath[1].replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
                ResultActions answer = mvc.perform(request(HttpMethod.valueOf(methodAndPath[0]), path)
                        .with(onHost(complex.host()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"));
                assertThat(answer.andReturn().getResponse().getStatus()).as(route).isEqualTo(403);
                answer.andExpect(jsonPath("$.code").value("TENANT_UNAVAILABLE"));
                blocked.add(route);
            }
        }
        return blocked;
    }

    private static List<String> routesOf(RequestMappingInfo info) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        List<String> routes = new ArrayList<>();
        for (String pattern : info.getPatternValues()) {
            for (RequestMethod method : methods.isEmpty() ? Set.of(RequestMethod.GET) : methods) {
                routes.add(method.name() + " " + pattern);
            }
        }
        return routes;
    }

    private static boolean isGlobalOperation(String route) {
        String path = route.split(" ", 2)[1];
        return path.equals(SPRING_ERROR_PATH) || GLOBAL_OPERATIONS.stream().anyMatch(path::startsWith);
    }

    /** The operations app (staff) and movil (players) use on the host of a complex. */
    private static List<String> appAndMovilRoutes() {
        return List.of(
                "GET /api/v1/tenant",
                "POST /api/v1/auth/staff/login",
                "GET /api/v1/staff/bookings",
                "POST /api/v1/staff/bookings",
                "POST /api/v1/staff/blocks",
                "GET /api/v1/courts/{courtId}/occupancy");
    }

    private Complex registeredComplex(String prefix) throws Exception {
        String subdomain = uniqueSubdomain(prefix);
        String id = json(api.register(superAdminToken, "Complex " + subdomain, subdomain,
                TestAccounts.uniqueEmail("owner"), planId).andExpect(status().isCreated())).get("id").asString();
        return new Complex(id, subdomain, mvc);
    }

    private ResultActions changeRetention(String token, String complexId, int days, long version) throws Exception {
        return api.as(token, put(COMPLEXES + "/" + complexId + "/retention"), """
                {"retentionDays": %d, "version": %d}
                """.formatted(days, version));
    }

    private ResultActions createBooking(Complex complex, String staffToken) throws Exception {
        return mvc.perform(post(STAFF_BOOKINGS).with(onHost(complex.host()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courtId": "%s", "start": "%s", "holderName": "Alex Example"}
                                """.formatted(courts.addCourt(Duration.ofMinutes(60)), FUTURE_SLOT)))
                .andExpect(status().isCreated());
    }

    private ResultActions listBookings(Complex complex, String staffToken) throws Exception {
        return mvc.perform(get(STAFF_BOOKINGS)
                .with(onHost(complex.host()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .param("from", "2030-01-01T00:00:00Z")
                .param("to", "2030-01-02T00:00:00Z"));
    }

    private ResultActions staffLogin(Complex complex) throws Exception {
        String email = TestAccounts.uniqueEmail("staff");
        TestAccounts.staff(complex.tenantId(), email, "ADMIN");
        return mvc.perform(post(STAFF_LOGIN).with(onHost(complex.host()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, TestAccounts.PASSWORD)));
    }

    /** The status changes of the complex after the activation of its registration. */
    private List<TenantStatusChanged> statusEventsOf(Complex complex) {
        return events.stream(TenantStatusChanged.class)
                .filter(event -> event.tenantId().equals(complex.tenantId()))
                .skip(1)
                .toList();
    }

    /** Whether the complex is visible in the global catalog; fails if it has no catalog row. */
    private static boolean catalogVisibilityOf(Complex complex) {
        return TenantDatabase.admin().sql("SELECT visible FROM global.complex_catalog WHERE tenant_id = :id::uuid")
                .param("id", complex.id())
                .query(Boolean.class)
                .single();
    }

    private static List<Map<String, Object>> bookingRowsOf(Complex complex) {
        return TenantDatabase.admin()
                .sql("SELECT * FROM \"%s\".booking ORDER BY id".formatted(TestTenants.schemaOf(complex.tenantId())))
                .query()
                .listOfRows();
    }

    private static void moveTenantDate(Complex complex, String column, Instant at) {
        TenantDatabase.admin().sql("UPDATE global.tenant SET %s = :at WHERE id = :id::uuid".formatted(column))
                .param("at", Timestamp.from(at))
                .param("id", complex.id())
                .update();
    }

    private static void setTenantStatus(Complex complex, String status) {
        TenantDatabase.admin().sql("UPDATE global.tenant SET status = :status WHERE id = :id::uuid")
                .param("status", status)
                .param("id", complex.id())
                .update();
    }

    private static void setOperatorCapability(String capability, boolean enabled) {
        TenantDatabase.admin()
                .sql("UPDATE global.operator_capability SET enabled = :enabled WHERE capability_key = :key")
                .param("enabled", enabled).param("key", capability).update();
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
    }

    /** A complex registered by the test, with its staff accounts created on demand. */
    private record Complex(String id, String subdomain, MockMvc mvc) {

        TenantId tenantId() {
            return new TenantId(id);
        }

        String host() {
            return subdomain + "." + TestTenants.ROOT_DOMAIN;
        }

        String staffToken() throws Exception {
            String email = TestAccounts.uniqueEmail("staff");
            TestAccounts.staff(tenantId(), email, "ADMIN");
            return json(mvc.perform(post(STAFF_LOGIN).with(onHost(host()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "%s", "password": "%s"}
                                    """.formatted(email, TestAccounts.PASSWORD)))
                    .andExpect(status().isOk()))
                    .get("accessToken").asString();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LifecycleEffectsTestConfiguration {

        @Bean
        StubCourtSlotRules courtSlotRules() {
            return new StubCourtSlotRules();
        }

        /** Makes the real catalog fail on demand right after it wrote, to prove the write is rolled back. */
        @Bean
        static BeanPostProcessor failingCatalog() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    return bean instanceof ComplexCatalog catalog ? new FailingCatalog(catalog) : bean;
                }
            };
        }
    }

    /** The real catalog, failing after hiding one of {@link #FAILING_TENANTS}. */
    static final class FailingCatalog implements ComplexCatalog {

        static final Set<UUID> FAILING_TENANTS = ConcurrentHashMap.newKeySet();
        static final String FAILURE = "Simulated failure of the catalog";

        private final ComplexCatalog delegate;

        FailingCatalog(ComplexCatalog delegate) {
            this.delegate = delegate;
        }

        @Override
        public void show(CatalogEntry entry, Instant at) {
            delegate.show(entry, at);
        }

        @Override
        public void hide(UUID tenantId, Instant at) {
            delegate.hide(tenantId, at);
            if (FAILING_TENANTS.contains(tenantId)) {
                throw new IllegalStateException(FAILURE);
            }
        }

        @Override
        public void changeSubdomain(UUID tenantId, String subdomain, Instant at) {
            delegate.changeSubdomain(tenantId, subdomain, at);
        }

        @Override
        public void remove(UUID tenantId) {
            delegate.remove(tenantId);
        }
    }
}
