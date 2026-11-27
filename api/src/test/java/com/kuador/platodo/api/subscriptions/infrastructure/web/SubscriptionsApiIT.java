package com.kuador.platodo.api.subscriptions.infrastructure.web;

import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.COMPLEXES;
import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.json;
import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.uniqueSubdomain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kuador.platodo.api.subscriptions.application.DeleteExpiredSubscriptionHistory;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionStatus;
import com.kuador.platodo.api.subscriptions.domain.event.SubscriptionPlanChanged;
import com.kuador.platodo.api.subscriptions.domain.event.SubscriptionStatusChanged;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;

import tools.jackson.databind.JsonNode;

/**
 * Tag «Subscriptions» of {@code contracts/platform-backoffice.openapi.yaml} over HTTP, with the real token filter,
 * guard and PostgreSQL (ADR-014, ADR-018; PLATODO-27 CA5, CA6 and CA7): plan changes, manual status changes,
 * immutable payments and the history with who made each change. Complexes are global and shared by every test of the
 * JVM, so each test registers complexes and plans of its own.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class SubscriptionsApiIT {

    private static final String STATUS_CHANGE = "SUBSCRIPTION_STATUS_CHANGE";
    private static final String PAYMENT_RECORD = "SUBSCRIPTION_PAYMENT_RECORD";
    /** Version of a complex right after its registration: tenant 1 (provisioned) + subscription 0. */
    private static final long REGISTERED_VERSION = 1;
    /** A status change with an effect on the tenant (suspend, cancel, reactivate) raises both versions. */
    private static final long SUBSCRIPTION_AND_TENANT_CHANGE = 2;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ApplicationEvents events;
    @Autowired
    private DeleteExpiredSubscriptionHistory deleteExpiredHistory;

    private ComplexRequests api;
    private String superAdminToken;
    private String planId;
    private String planName;

    @BeforeEach
    void setUp() throws Exception {
        api = new ComplexRequests(mvc);
        ComplexRequests.allowTestPlanOptions();
        superAdminToken = api.superAdminToken();
        planName = "Basic " + UUID.randomUUID();
        planId = api.publishedPlan(superAdminToken, planName);
    }

    @AfterEach
    void tearDown() {
        ComplexRequests.restorePlanOptions();
        setOperatorCapability(STATUS_CHANGE, false);
        setOperatorCapability(PAYMENT_RECORD, false);
        FailingStatusConsumer.FAILING_TENANTS.clear();
    }

    /** CA5: the new snapshot is assigned and the history keeps the previous plan, the new one, when and who. */
    @Test
    void aSuperAdminChangesThePlanAndTheHistoryKeepsIt() throws Exception {
        String complexId = registeredComplex("plan-change");
        String premiumName = "Premium " + UUID.randomUUID();
        String premiumId = api.publishedPlan(superAdminToken, premiumName);

        api.changePlan(superAdminToken, complexId, premiumId, REGISTERED_VERSION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.plan.planId").value(premiumId))
                .andExpect(jsonPath("$.subscription.plan.name").value(premiumName))
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(REGISTERED_VERSION + 1));

        JsonNode history = json(api.history(superAdminToken, complexId).andExpect(status().isOk()))
                .get("items");
        assertThat(history).hasSize(3);
        JsonNode planChange = history.get(0);
        assertThat(planChange.get("kind").asString()).isEqualTo("PLAN");
        assertThat(planChange.get("before").asString()).isEqualTo(planName);
        assertThat(planChange.get("after").asString()).isEqualTo(premiumName);
        assertThat(planChange.get("changedBy").get("displayName").asString())
                .isEqualTo(TestAccounts.PLATFORM_USER_NAME);
        assertThat(planChange.get("changedAt").asString()).isNotBlank();
        assertThat(kindsAndValues(history.get(1), history.get(2))).containsExactlyInAnyOrder(
                "PLAN:null->" + planName, "STATUS:null->ACTIVE");
        assertThat(events.stream(SubscriptionPlanChanged.class))
                .anySatisfy(event -> {
                    assertThat(event.tenantId()).hasToString(complexId);
                    assertThat(event.fromPlanId()).hasToString(planId);
                    assertThat(event.toPlanId()).hasToString(premiumId);
                });
    }

    @Test
    void aPlanChangeIsRejectedForTheSamePlanADraftOrAStaleVersion() throws Exception {
        String complexId = registeredComplex("plan-rejected");
        String draftId = api.draftPlan(superAdminToken, "Draft " + UUID.randomUUID());
        String otherId = api.publishedPlan(superAdminToken, "Other " + UUID.randomUUID());

        expectProblem(api.changePlan(superAdminToken, complexId, planId, REGISTERED_VERSION), 422, "SAME_PLAN");
        expectProblem(api.changePlan(superAdminToken, complexId, draftId, REGISTERED_VERSION), 422,
                "PLAN_NOT_PUBLISHED");
        expectProblem(api.changePlan(superAdminToken, complexId, UUID.randomUUID().toString(), REGISTERED_VERSION),
                422, "PLAN_NOT_PUBLISHED");
        expectProblem(api.changePlan(superAdminToken, complexId, otherId, REGISTERED_VERSION + 5), 409,
                "STALE_STATE");
        expectProblem(api.changePlan(superAdminToken, UUID.randomUUID().toString(), otherId, 0), 404,
                "COMPLEX_NOT_FOUND");
        expectProblem(api.as(superAdminToken, put(COMPLEXES + "/" + complexId + "/plan"),
                "{\"planId\": \"%s\"}".formatted(otherId)), 400, "INVALID_REQUEST");

        api.detail(superAdminToken, complexId)
                .andExpect(jsonPath("$.subscription.plan.planId").value(planId))
                .andExpect(jsonPath("$.version").value(REGISTERED_VERSION));
        assertThat(historyOf(complexId)).hasSize(2);
    }

    /** ADR-014: a cancelled subscription keeps its plan. */
    @Test
    void aCancelledSubscriptionKeepsItsPlan() throws Exception {
        String complexId = registeredComplex("plan-cancelled");
        String otherId = api.publishedPlan(superAdminToken, "Other " + UUID.randomUUID());
        api.changeStatus(superAdminToken, complexId, "CANCELLED", REGISTERED_VERSION).andExpect(status().isOk());

        expectProblem(api.changePlan(superAdminToken, complexId, otherId,
                REGISTERED_VERSION + SUBSCRIPTION_AND_TENANT_CHANGE), 422, "SUBSCRIPTION_CANCELLED");
    }

    /** CA6: every allowed transition is applied by hand, with when and who, and CANCELLED is terminal. */
    @Test
    void statusChangesFollowTheTableAndCancelledIsTerminal() throws Exception {
        String complexId = registeredComplex("status-table");
        long version = REGISTERED_VERSION;
        for (String target : List.of("EXPIRING", "ACTIVE", "SUSPENDED", "CANCELLED")) {
            long previous = version;
            version = json(api.changeStatus(superAdminToken, complexId, target, version)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subscription.status").value(target))
                    .andExpect(jsonPath("$.subscription.statusChangedBy.displayName")
                            .value(TestAccounts.PLATFORM_USER_NAME)))
                    .get("version").asLong();
            assertThat(version).isGreaterThan(previous);
        }

        api.detail(superAdminToken, complexId).andExpect(jsonPath("$.allowedTransitions").value(empty()));
        expectProblem(api.changeStatus(superAdminToken, complexId, "ACTIVE", version), 422,
                "TRANSITION_NOT_ALLOWED");

        JsonNode history = json(api.history(superAdminToken, complexId)).get("items");
        List<String> statusChanges = new ArrayList<>();
        history.forEach(entry -> {
            if (entry.get("kind").asString().equals("STATUS")) {
                statusChanges.add(entry.get("before").asString(null) + "->" + entry.get("after").asString());
            }
        });
        assertThat(statusChanges).containsExactly("SUSPENDED->CANCELLED", "ACTIVE->SUSPENDED", "EXPIRING->ACTIVE",
                "ACTIVE->EXPIRING", "null->ACTIVE");
        assertThat(events.stream(SubscriptionStatusChanged.class)
                .filter(event -> event.tenantId().toString().equals(complexId))
                .map(event -> event.from() + "->" + event.to()))
                .containsExactly("ACTIVE->EXPIRING", "EXPIRING->ACTIVE", "ACTIVE->SUSPENDED",
                        "SUSPENDED->CANCELLED");
    }

    @Test
    void aTransitionOutsideTheTableOrWithAStaleVersionChangesNothing() throws Exception {
        String complexId = registeredComplex("status-rejected");
        long suspended = json(api.changeStatus(superAdminToken, complexId, "SUSPENDED", REGISTERED_VERSION)
                .andExpect(status().isOk())).get("version").asLong();

        expectProblem(api.changeStatus(superAdminToken, complexId, "EXPIRING", suspended), 422,
                "TRANSITION_NOT_ALLOWED");
        expectProblem(api.changeStatus(superAdminToken, complexId, "SUSPENDED", suspended), 422,
                "TRANSITION_NOT_ALLOWED");
        expectProblem(api.changeStatus(superAdminToken, complexId, "ACTIVE", REGISTERED_VERSION), 409,
                "STALE_STATE");
        expectProblem(api.changeStatus(superAdminToken, complexId, "PAUSED", suspended), 400,
                "INVALID_REQUEST");

        api.detail(superAdminToken, complexId)
                .andExpect(jsonPath("$.subscription.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.version").value(suspended));
    }

    /** Optimistic concurrency: two changes made from the same version apply exactly one. */
    @Test
    void concurrentStatusChangesFromTheSameVersionApplyOnlyOne() throws Exception {
        String complexId = registeredComplex("status-race");
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> suspend = changeStatusWhenReleased(start, complexId, "SUSPENDED");
        Callable<Integer> cancel = changeStatusWhenReleased(start, complexId, "CANCELLED");

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(suspend);
            Future<Integer> second = executor.submit(cancel);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
        }

        assertThat(historyOf(complexId).stream().filter(change -> change.startsWith("STATUS:ACTIVE->"))).hasSize(1);
        api.detail(superAdminToken, complexId)
                .andExpect(jsonPath("$.version").value(REGISTERED_VERSION + SUBSCRIPTION_AND_TENANT_CHANGE));
    }

    /**
     * ADR-014: synchronous consumers of {@link SubscriptionStatusChanged} run in the change transaction, so if one
     * fails nothing of the change is kept.
     */
    @Test
    void aFailingSynchronousConsumerRollsTheStatusChangeBack() throws Exception {
        String complexId = registeredComplex("status-rollback");
        FailingStatusConsumer.FAILING_TENANTS.add(UUID.fromString(complexId));

        assertThatThrownBy(() -> api.changeStatus(superAdminToken, complexId, "SUSPENDED", REGISTERED_VERSION))
                .rootCause().hasMessage(FailingStatusConsumer.FAILURE);

        api.detail(superAdminToken, complexId)
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(REGISTERED_VERSION));
        assertThat(historyOf(complexId)).hasSize(2);
    }

    /** CA6 and CA15: operators read history, and change statuses only with SUBSCRIPTION_STATUS_CHANGE. */
    @Test
    void operatorsChangeStatusesOnlyWithTheCapabilityAndNeverThePlan() throws Exception {
        String operatorToken = api.operatorToken();
        String complexId = registeredComplex("status-operator");
        String otherId = api.publishedPlan(superAdminToken, "Other " + UUID.randomUUID());

        api.history(operatorToken, complexId).andExpect(status().isOk());
        expectProblem(api.changeStatus(operatorToken, complexId, "EXPIRING", REGISTERED_VERSION), 403,
                "CAPABILITY_REVOKED");
        expectProblem(api.changePlan(operatorToken, complexId, otherId, REGISTERED_VERSION), 403, "FORBIDDEN");

        setOperatorCapability(STATUS_CHANGE, true);

        api.changeStatus(operatorToken, complexId, "EXPIRING", REGISTERED_VERSION).andExpect(status().isOk());
        expectProblem(api.changePlan(operatorToken, complexId, otherId, REGISTERED_VERSION + 1), 403, "FORBIDDEN");
    }

    /** CA7: a payment is recorded with amount, date and method, in the currency of the plan snapshot. */
    @Test
    void aPaymentIsRecordedInTheCurrencyOfThePlanSnapshot() throws Exception {
        String complexId = registeredComplex("payment");
        api.revisePlan(superAdminToken, planId, planName, "EUR", 2);

        api.recordPayment(superAdminToken, complexId, "120.5", "2026-11-02", "TRANSFER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.amount").value("120.50"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.paidOn").value("2026-11-02"))
                .andExpect(jsonPath("$.method").value("TRANSFER"))
                .andExpect(jsonPath("$.recordedBy.displayName").value(TestAccounts.PLATFORM_USER_NAME))
                .andExpect(jsonPath("$.recordedAt").isString());
        api.recordPayment(superAdminToken, complexId, "80", "2026-11-20", "INVOICE").andExpect(status().isCreated());
        api.recordPayment(superAdminToken, complexId, "99.9900", "2026-10-01", "TRANSFER")
                .andExpect(status().isCreated());

        JsonNode page = json(api.payments(superAdminToken, complexId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)));
        List<String> paidOn = new ArrayList<>();
        page.get("items").forEach(item -> paidOn.add(item.get("paidOn").asString()));
        assertThat(paidOn).containsExactly("2026-11-20", "2026-11-02", "2026-10-01");
        api.detail(superAdminToken, complexId).andExpect(jsonPath("$.version").value(REGISTERED_VERSION));
    }

    @Test
    void invalidPaymentsAreReportedTogetherAndNothingIsRecorded() throws Exception {
        String complexId = registeredComplex("payment-invalid");
        String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();

        expectProblem(api.recordPayment(superAdminToken, complexId, "0", tomorrow, "CASH"), 422,
                "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[*].field").value(containsInAnyOrder("amount", "paidOn", "method")))
                .andExpect(jsonPath("$.fieldErrors[*].code").value(
                        containsInAnyOrder("NOT_POSITIVE", "DATE_IN_FUTURE", "NOT_ALLOWED")));
        expectProblem(api.recordPayment(superAdminToken, complexId, "1.123456", "2026-11-01", "TRANSFER"), 422,
                "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_FORMAT"));
        expectProblem(api.recordPayment(superAdminToken, complexId, "10", "01/11/2026", "TRANSFER"), 400,
                "INVALID_REQUEST");
        expectProblem(api.recordPayment(superAdminToken, UUID.randomUUID().toString(), "10", "2026-11-01",
                "TRANSFER"), 404, "COMPLEX_NOT_FOUND");

        api.payments(superAdminToken, complexId).andExpect(jsonPath("$.totalItems").value(0));
    }

    /** CA7: payments are immutable; there is no operation to edit or delete one. */
    @Test
    void aRecordedPaymentCannotBeEditedNorDeleted() throws Exception {
        String complexId = registeredComplex("payment-immutable");
        String paymentId = json(api.recordPayment(superAdminToken, complexId, "50", "2026-11-01", "TRANSFER")
                .andExpect(status().isCreated())).get("id").asString();
        String paymentPath = COMPLEXES + "/" + complexId + "/payments/" + paymentId;

        api.as(superAdminToken, put(paymentPath), "{\"amount\": \"1\", \"paidOn\": \"2026-11-01\"}")
                .andExpect(status().is4xxClientError());
        api.as(superAdminToken, delete(paymentPath), null).andExpect(status().is4xxClientError());

        api.payments(superAdminToken, complexId)
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].amount").value("50.00"));
    }

    /** CA7 and CA15: operators read payments, and record them only with SUBSCRIPTION_PAYMENT_RECORD. */
    @Test
    void operatorsRecordPaymentsOnlyWithTheCapability() throws Exception {
        String operatorToken = api.operatorToken();
        String complexId = registeredComplex("payment-operator");

        api.payments(operatorToken, complexId).andExpect(status().isOk());
        expectProblem(api.recordPayment(operatorToken, complexId, "10", "2026-11-01", "TRANSFER"), 403,
                "CAPABILITY_REVOKED");

        setOperatorCapability(PAYMENT_RECORD, true);

        api.recordPayment(operatorToken, complexId, "10", "2026-11-01", "TRANSFER")
                .andExpect(status().isCreated());
    }

    /** CA5: the history of the last 365 days is returned; older entries never are. */
    @Test
    void historyOlderThanOneYearIsNotReturned() throws Exception {
        String complexId = registeredComplex("history-retention");
        insertHistoryEntry(complexId, "PLAN", "Old plan", "Older plan", "366 days");
        insertHistoryEntry(complexId, "STATUS", "ACTIVE", "EXPIRING", "364 days");

        JsonNode page = json(api.history(superAdminToken, complexId).andExpect(status().isOk()));

        assertThat(page.get("totalItems").asLong()).isEqualTo(3);
        assertThat(historyOf(complexId)).contains("STATUS:ACTIVE->EXPIRING").doesNotContain("PLAN:Old plan->Older plan");
    }

    /** CA5 and ADR-016: the retention job deletes the history past one year and keeps the rest and the payments. */
    @Test
    void theRetentionJobDeletesOnlyTheHistoryOlderThanOneYear() throws Exception {
        String complexId = registeredComplex("history-purge");
        insertHistoryEntry(complexId, "PLAN", "Old plan", "Older plan", "366 days");
        insertHistoryEntry(complexId, "STATUS", "ACTIVE", "EXPIRING", "364 days");
        api.recordPayment(superAdminToken, complexId, "10", "2026-11-01", "TRANSFER").andExpect(status().isCreated());

        deleteExpiredHistory.run();

        assertThat(historyRowsOf(complexId)).isEqualTo(3);
        assertThat(historyOf(complexId)).contains("STATUS:ACTIVE->EXPIRING");
        assertThat(json(api.payments(superAdminToken, complexId)).get("totalItems").asLong()).isEqualTo(1);
    }

    /**
     * Isolation: changing the plan, status and payments of one complex leaves another complex untouched, and the
     * payments and history of each complex show only its own.
     */
    @Test
    void changesOnOneComplexDoNotTouchAnother() throws Exception {
        String changedId = registeredComplex("isolated-changed");
        String untouchedId = registeredComplex("isolated-untouched");
        JsonNode untouchedBefore = json(api.detail(superAdminToken, untouchedId));
        String otherId = api.publishedPlan(superAdminToken, "Other " + UUID.randomUUID());

        api.changePlan(superAdminToken, changedId, otherId, REGISTERED_VERSION).andExpect(status().isOk());
        api.changeStatus(superAdminToken, changedId, "SUSPENDED", REGISTERED_VERSION + 1).andExpect(status().isOk());
        api.recordPayment(superAdminToken, changedId, "10", "2026-11-01", "TRANSFER").andExpect(status().isCreated());

        assertThat(json(api.detail(superAdminToken, untouchedId))).isEqualTo(untouchedBefore);
        api.payments(superAdminToken, untouchedId).andExpect(jsonPath("$.totalItems").value(0));
        assertThat(historyOf(untouchedId)).containsExactlyInAnyOrder("PLAN:null->" + planName, "STATUS:null->ACTIVE");
        assertThat(historyOf(changedId)).hasSize(4);
    }

    @Test
    void playersAndUnknownComplexesNeverReachSubscriptions() throws Exception {
        String playerToken = api.playerToken();
        String complexId = registeredComplex("guarded");

        expectProblem(api.payments(playerToken, complexId), 403, "FORBIDDEN");
        expectProblem(api.history(playerToken, complexId), 403, "FORBIDDEN");
        expectProblem(api.changeStatus(playerToken, complexId, "SUSPENDED", REGISTERED_VERSION), 403, "FORBIDDEN");
        expectProblem(api.recordPayment(playerToken, complexId, "1", "2026-11-01", "TRANSFER"), 403, "FORBIDDEN");
        expectProblem(api.payments(superAdminToken, UUID.randomUUID().toString()), 404, "COMPLEX_NOT_FOUND");
        expectProblem(api.history(superAdminToken, UUID.randomUUID().toString()), 404, "COMPLEX_NOT_FOUND");
        expectProblem(api.changeStatus(superAdminToken, UUID.randomUUID().toString(), "SUSPENDED", 0), 404,
                "COMPLEX_NOT_FOUND");
    }

    private Callable<Integer> changeStatusWhenReleased(CountDownLatch start, String complexId, String target) {
        return () -> {
            start.await();
            return api.changeStatus(superAdminToken, complexId, target, REGISTERED_VERSION)
                    .andReturn().getResponse().getStatus();
        };
    }

    private String registeredComplex(String prefix) throws Exception {
        String subdomain = uniqueSubdomain(prefix);
        return json(api.register(superAdminToken, "Complex " + subdomain, subdomain,
                TestAccounts.uniqueEmail("owner"), planId).andExpect(status().isCreated())).get("id").asString();
    }

    /** Every history entry of the complex returned by the api, as {@code KIND:before->after}, newest first. */
    private List<String> historyOf(String complexId) throws Exception {
        List<String> entries = new ArrayList<>();
        json(api.history(superAdminToken, complexId).andExpect(status().isOk())).get("items")
                .forEach(entry -> entries.add(kindsAndValues(entry).getFirst()));
        return entries;
    }

    private static List<String> kindsAndValues(JsonNode... entries) {
        List<String> values = new ArrayList<>();
        for (JsonNode entry : entries) {
            values.add(entry.get("kind").asString() + ":" + entry.get("before").asString(null) + "->"
                    + entry.get("after").asString());
        }
        return values;
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
    }

    private static long historyRowsOf(String complexId) {
        return TenantDatabase.admin()
                .sql("SELECT count(*) FROM global.subscription_history WHERE tenant_id = CAST(:id AS uuid)")
                .param("id", complexId)
                .query(Long.class).single();
    }

    private static void insertHistoryEntry(String complexId, String kind, String before, String after, String age) {
        TenantDatabase.admin().sql("""
                        INSERT INTO global.subscription_history
                            (id, tenant_id, kind, before, after, changed_by, changed_at)
                        SELECT gen_random_uuid(), s.tenant_id, :kind, :before, :after, s.status_changed_by,
                               now() - CAST(:age AS interval)
                        FROM global.subscription s WHERE s.tenant_id = CAST(:tenantId AS uuid)
                        """)
                .param("kind", kind)
                .param("before", before)
                .param("after", after)
                .param("age", age)
                .param("tenantId", complexId)
                .update();
    }

    private static void setOperatorCapability(String capability, boolean enabled) {
        TenantDatabase.admin().sql("""
                        UPDATE global.operator_capability SET enabled = :enabled, version = version + 1
                        WHERE capability_key = :capability AND enabled <> :enabled
                        """)
                .param("enabled", enabled).param("capability", capability).update();
    }

    /** A synchronous consumer, like Tenancy, that fails for the complexes the test chooses. */
    @TestConfiguration(proxyBeanMethods = false)
    static class FailingStatusConsumer {

        static final Set<UUID> FAILING_TENANTS = ConcurrentHashMap.newKeySet();
        static final String FAILURE = "Simulated failure of a synchronous consumer";

        @EventListener
        void on(SubscriptionStatusChanged event) {
            if (FAILING_TENANTS.contains(event.tenantId()) && event.to() == SubscriptionStatus.SUSPENDED) {
                throw new IllegalStateException(FAILURE);
            }
        }
    }
}
