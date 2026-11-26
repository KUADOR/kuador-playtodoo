package com.kuador.platodo.api.subscriptions.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tags «Plans» and «Plan fields» of {@code contracts/platform-backoffice.openapi.yaml} over HTTP, with the real token
 * filter, guard and PostgreSQL (ADR-018; PLATODO-27 CA3 and CA4). Plans and fields are global and shared by every
 * test of the JVM, so each test uses names of its own and retires its custom fields afterwards. The plan options are
 * set for the tests and restored to the empty values of the migration afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class PlansApiIT {

    private static final String PLATFORM = "/api/v1/platform";
    private static final String PLANS = PLATFORM + "/plans";
    private static final String PLAN_OPTIONS = PLATFORM + "/plan-options";
    private static final String PLAN_FIELDS = PLATFORM + "/plan-fields";
    private static final String PLATFORM_LOGIN = "/api/v1/auth/platform/login";
    private static final String PLAYER_LOGIN = "/api/v1/auth/players/login";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TEST_CURRENCIES = "[\"USD\"]";
    private static final String TEST_PERIODICITIES = "[\"MONTHLY\", \"YEARLY\"]";
    private static final String NO_VALUES = "[]";

    @Autowired
    private MockMvc mvc;

    private String superAdminToken;
    private String operatorToken;

    @BeforeEach
    void setUp() throws Exception {
        setPlanOptions(TEST_CURRENCIES, TEST_PERIODICITIES);
        String superAdminEmail = TestAccounts.uniqueEmail("plans-super-admin");
        TestAccounts.platformOwner(superAdminEmail);
        superAdminToken = logIn(PLATFORM_LOGIN, superAdminEmail);
        String operatorEmail = TestAccounts.uniqueEmail("plans-operator");
        TestAccounts.operator(operatorEmail);
        operatorToken = logIn(PLATFORM_LOGIN, operatorEmail);
    }

    /** Restores the plan options and retires the custom fields of the test, so they require nothing elsewhere. */
    @AfterEach
    void cleanUp() {
        setPlanOptions(NO_VALUES, NO_VALUES);
        TenantDatabase.admin().sql("""
                        UPDATE global.plan_field_definition SET retired_at = now()
                        WHERE origin = 'CUSTOM' AND retired_at IS NULL
                        """)
                .update();
    }

    /** CA3: a draft needs only a name; publishing requires every basic field and the required custom fields. */
    @Test
    void aDraftIsPublishedOnlyOnceItIsComplete() throws Exception {
        String requiredField = createField(unique("Support hours"), "TEXT", true);
        String name = unique("Premium");

        JsonNode draft = json(asSuperAdmin(post(PLANS), """
                {"name": "%s"}
                """.formatted(name))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(PLANS + "/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.price").value(nullValue()))
                .andExpect(jsonPath("$.courtLimit").value(nullValue()))
                .andExpect(jsonPath("$.customFields").isMap()));
        String planId = draft.get("id").asString();

        asSuperAdmin(post(PLANS + "/" + planId + "/publication"), null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_INCOMPLETE"))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(containsInAnyOrder("price", "currency",
                        "periodicity", "courtLimit", "userLimit", "customFields." + requiredField)))
                .andExpect(jsonPath("$.fieldErrors[*].code").value(everyItem(is("REQUIRED"))));

        asSuperAdmin(put(PLANS + "/" + planId), completePlan(name, 0, requiredField, "Weekdays 9 to 18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value("120.00"))
                .andExpect(jsonPath("$.customFields['" + requiredField + "']").value("Weekdays 9 to 18"))
                .andExpect(jsonPath("$.version").value(1));

        asSuperAdmin(post(PLANS + "/" + planId + "/publication"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.version").value(2));
        asSuperAdmin(post(PLANS + "/" + planId + "/publication"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    /** CA3: operators, who register complexes, only see published plans. */
    @Test
    void operatorsSeeOnlyPublishedPlans() throws Exception {
        String draftId = createPlan(unique("Draft only"));
        String publishedName = unique("Published");
        String publishedId = createPublishedPlan(publishedName);

        as(operatorToken, get(PLANS + "/" + publishedId), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(publishedName));
        as(operatorToken, get(PLANS + "/" + draftId), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
        as(operatorToken, get(PLANS).param("status", "PUBLISHED").param("size", "100"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].status").value(everyItem(is("PUBLISHED"))))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100));
        as(operatorToken, get(PLANS), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        as(operatorToken, get(PLANS).param("status", "DRAFT"), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        asSuperAdmin(get(PLANS + "/" + draftId), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void theSuperAdminListsEveryPlanByName() throws Exception {
        String prefix = unique("Order");
        createPlan(prefix + " b");
        createPlan(prefix + " A");

        JsonNode page = json(asSuperAdmin(get(PLANS).param("size", "100"), null).andExpect(status().isOk()));
        List<String> names = new ArrayList<>();
        page.get("items").forEach(item -> names.add(item.get("name").asString()));

        assertThat(names).containsSubsequence(prefix + " A", prefix + " b");
        assertThat(page.get("totalItems").asLong()).isGreaterThanOrEqualTo(2);
    }

    /** Plan operations other than reading published plans and the options are SUPER_ADMIN only. */
    @Test
    void operatorsCannotChangePlans() throws Exception {
        String planId = createPlan(unique("Guarded"));

        as(operatorToken, post(PLANS), "{\"name\": \"%s\"}".formatted(unique("By operator")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        as(operatorToken, put(PLANS + "/" + planId), "{\"name\": \"x\", \"version\": 0}")
                .andExpect(status().isForbidden());
        as(operatorToken, post(PLANS + "/" + planId + "/publication"), null).andExpect(status().isForbidden());
        for (MockHttpServletRequestBuilder request : List.of(get(PLAN_FIELDS),
                post(PLAN_FIELDS).content("{\"label\": \"x\", \"type\": \"TEXT\", \"required\": false}"))) {
            as(operatorToken, request, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Test
    void playersAndAnonymousCallersDoNotReachPlans() throws Exception {
        String playerEmail = TestAccounts.uniqueEmail("plans-player");
        TestAccounts.player(playerEmail);
        String playerToken = logIn(PLAYER_LOGIN, playerEmail);

        for (String path : List.of(PLANS + "?status=PUBLISHED", PLAN_OPTIONS, PLAN_FIELDS)) {
            as(playerToken, get(path), null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            mvc.perform(get(path).with(onHost(TestTenants.GLOBAL_HOST)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
    }

    @Test
    void planOptionsComeFromThePlatformParameters() throws Exception {
        as(operatorToken, get(PLAN_OPTIONS), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencies[0]").value("USD"))
                .andExpect(jsonPath("$.periodicities[1]").value("YEARLY"));

        setPlanOptions("[\"USD\", \"PEN\"]", "[\"MONTHLY\"]");

        as(operatorToken, get(PLAN_OPTIONS), null)
                .andExpect(jsonPath("$.currencies[1]").value("PEN"))
                .andExpect(jsonPath("$.periodicities.length()").value(1));
    }

    @Test
    void invalidFieldsAreReportedTogether() throws Exception {
        String flag = createField(unique("Tournaments included"), "BOOLEAN", false);

        asSuperAdmin(post(PLANS), """
                {"name": "%s", "price": "12.345678", "currency": "EUR", "periodicity": "WEEKLY",
                 "courtLimit": -1, "customFields": {"%s": "yes", "not-a-field": 1}}
                """.formatted(unique("Invalid"), flag))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'price')].code").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'currency')].code").value("NOT_ALLOWED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'periodicity')].code").value("NOT_ALLOWED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'courtLimit')].code").value("NEGATIVE"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'customFields.%s')].code".formatted(flag))
                        .value("WRONG_TYPE"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'customFields.not-a-field')].code")
                        .value("UNKNOWN_FIELD"));
        asSuperAdmin(post(PLANS), "{\"price\": \"10\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED"));
    }

    @Test
    void planNamesAreUniqueWithoutRegardToCase() throws Exception {
        String name = unique("Unique");
        createPlan(name);
        String otherId = createPlan(unique("Other"));

        asSuperAdmin(post(PLANS), "{\"name\": \"%s\"}".formatted(name.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_NAME_TAKEN"));
        asSuperAdmin(put(PLANS + "/" + otherId), "{\"name\": \"%s\", \"version\": 0}".formatted(name.toLowerCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_NAME_TAKEN"));
    }

    /** Two creations with the same name at once: the unique index lets exactly one through. */
    @Test
    void concurrentCreationsWithTheSameNameCreateOnePlan() throws Exception {
        String name = unique("Concurrent");
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> create = () -> {
            start.await();
            return asSuperAdmin(post(PLANS), "{\"name\": \"%s\"}".formatted(name))
                    .andReturn().getResponse().getStatus();
        };
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(create);
            Future<Integer> second = executor.submit(create);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
    }

    @Test
    void aStaleVersionAnswersConflictAndChangesNothing() throws Exception {
        String name = unique("Stale");
        String planId = createPlan(name);
        asSuperAdmin(put(PLANS + "/" + planId), "{\"name\": \"%s\", \"version\": 0}".formatted(name + " v1"))
                .andExpect(status().isOk());

        asSuperAdmin(put(PLANS + "/" + planId), "{\"name\": \"%s\", \"version\": 0}".formatted(name + " v2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_STATE"));

        asSuperAdmin(get(PLANS + "/" + planId), null)
                .andExpect(jsonPath("$.name").value(name + " v1"))
                .andExpect(jsonPath("$.version").value(1));
    }

    /** ADR-018: editing a published plan keeps it published, and it must stay complete. */
    @Test
    void aPublishedPlanStaysPublishedAndComplete() throws Exception {
        String name = unique("Editable");
        String planId = createPublishedPlan(name);

        asSuperAdmin(put(PLANS + "/" + planId), """
                {"name": "%s", "price": "150.5", "currency": "USD", "periodicity": "YEARLY",
                 "courtLimit": 12, "userLimit": 30, "version": 2}
                """.formatted(name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.price").value("150.50"))
                .andExpect(jsonPath("$.version").value(3));
        asSuperAdmin(put(PLANS + "/" + planId), "{\"name\": \"%s\", \"version\": 3}".formatted(name))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_INCOMPLETE"));
    }

    /** CA4: custom fields are created and listed without code changes; basic fields come first. */
    @Test
    void customFieldsAreCreatedAndListedAfterTheBasicOnes() throws Exception {
        String label = unique("Horas de atención");

        asSuperAdmin(post(PLAN_FIELDS), "{\"label\": \"%s\", \"type\": \"TEXT\", \"required\": false}"
                        .formatted(label))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(matchesPattern("horas-de-atencion-[0-9a-f]{8}")))
                .andExpect(jsonPath("$.label").value(label))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.origin").value("CUSTOM"))
                .andExpect(jsonPath("$.retiredAt").value(nullValue()))
                .andExpect(jsonPath("$.version").value(0));

        JsonNode fields = json(asSuperAdmin(get(PLAN_FIELDS), null).andExpect(status().isOk()));
        List<String> keys = new ArrayList<>();
        fields.forEach(field -> keys.add(field.get("key").asString()));
        assertThat(keys.subList(0, 6))
                .containsExactly("name", "price", "currency", "periodicity", "courtLimit", "userLimit");
        assertThat(fields.get(1).get("origin").asString()).isEqualTo("BASIC");
        assertThat(fields.get(1).get("type").asString()).isEqualTo("NUMBER");
        assertThat(keys).anySatisfy(key -> assertThat(key).startsWith("horas-de-atencion-"));
    }

    @Test
    void fieldLabelsAreUniqueWithoutRegardToCase() throws Exception {
        String label = unique("Parking");
        createField(label, "BOOLEAN", false);

        asSuperAdmin(post(PLAN_FIELDS), "{\"label\": \"%s\", \"type\": \"TEXT\", \"required\": false}"
                        .formatted(label.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FIELD_NAME_TAKEN"));
        asSuperAdmin(post(PLAN_FIELDS), "{\"label\": \"price\", \"type\": \"NUMBER\", \"required\": false}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FIELD_NAME_TAKEN"));
    }

    /** CA4: a custom field's label and required flag change; its type never does. Basic fields are read-only. */
    @Test
    void customFieldsAreEditedWithVersionsAndBasicFieldsAreReadOnly() throws Exception {
        String label = unique("Lockers");
        JsonNode created = json(asSuperAdmin(post(PLAN_FIELDS),
                "{\"label\": \"%s\", \"type\": \"NUMBER\", \"required\": false}".formatted(label)));
        String fieldId = created.get("id").asString();

        asSuperAdmin(put(PLAN_FIELDS + "/" + fieldId), """
                {"label": "%s", "required": true, "version": 0}
                """.formatted(label + " total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value(label + " total"))
                .andExpect(jsonPath("$.key").value(created.get("key").asString()))
                .andExpect(jsonPath("$.type").value("NUMBER"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.version").value(1));
        asSuperAdmin(put(PLAN_FIELDS + "/" + fieldId), "{\"label\": \"%s\", \"required\": false, \"version\": 0}"
                        .formatted(label))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_STATE"));
        asSuperAdmin(put(PLAN_FIELDS + "/" + fieldId), "{\"label\": \" \", \"required\": false, \"version\": 1}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("label"));

        String priceId = fieldIdOf("price");
        asSuperAdmin(put(PLAN_FIELDS + "/" + priceId), "{\"label\": \"Cost\", \"required\": true, \"version\": 0}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BASIC_FIELD_READ_ONLY"));
        asSuperAdmin(post(PLAN_FIELDS + "/" + priceId + "/retirement"), null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BASIC_FIELD_READ_ONLY"));
    }

    /** CA4: a retired field is no longer offered nor required, and the values plans have are kept. */
    @Test
    void retiringAFieldKeepsExistingValuesAndStopsRequiringIt() throws Exception {
        String key = createField(unique("Legacy perk"), "TEXT", true);
        String fieldId = fieldIdOf(key);
        String name = unique("With perk");
        String planId = createPlan(name);
        asSuperAdmin(put(PLANS + "/" + planId), completePlan(name, 0, key, "Free balls")).andExpect(status().isOk());

        asSuperAdmin(post(PLAN_FIELDS + "/" + fieldId + "/retirement"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retiredAt").isString())
                .andExpect(jsonPath("$.version").value(1));
        asSuperAdmin(post(PLAN_FIELDS + "/" + fieldId + "/retirement"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        asSuperAdmin(put(PLANS + "/" + planId), completePlan(name, 1, key, "Other value"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("customFields." + key))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("NOT_ALLOWED"));
        asSuperAdmin(put(PLANS + "/" + planId), completePlan(name, 1, key, "Free balls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customFields['" + key + "']").value("Free balls"));

        createPublishedPlan(unique("Without perk"));
    }

    @Test
    void malformedRequestsAreInvalid() throws Exception {
        String planId = createPlan(unique("Malformed"));

        asSuperAdmin(put(PLANS + "/" + planId), "{\"name\": \"x\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        asSuperAdmin(post(PLANS), "{\"name\": \"x\", \"customFields\": [1]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        asSuperAdmin(post(PLANS), "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        asSuperAdmin(post(PLAN_FIELDS), "{\"label\": \"x\", \"type\": \"DATE\", \"required\": false}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        asSuperAdmin(post(PLAN_FIELDS), "{\"label\": \"x\", \"required\": false}")
                .andExpect(status().isBadRequest());
        asSuperAdmin(get(PLANS).param("size", "101"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        asSuperAdmin(get(PLANS + "/" + UUID.randomUUID()), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
        asSuperAdmin(post(PLAN_FIELDS + "/" + UUID.randomUUID() + "/retirement"), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_FIELD_NOT_FOUND"));
    }

    @Test
    void planOperationsAreNotServedOnATenantHost() throws Exception {
        mvc.perform(get(PLAN_OPTIONS).with(onHost(TestTenants.hostOf(TestTenants.A)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    private String createPlan(String name) throws Exception {
        return json(asSuperAdmin(post(PLANS), "{\"name\": \"%s\"}".formatted(name))
                .andExpect(status().isCreated())).get("id").asString();
    }

    private String createPublishedPlan(String name) throws Exception {
        String planId = createPlan(name);
        asSuperAdmin(put(PLANS + "/" + planId), completePlanWithoutCustomFields(name)).andExpect(status().isOk());
        asSuperAdmin(post(PLANS + "/" + planId + "/publication"), null).andExpect(status().isOk());
        return planId;
    }

    /** The key of a new custom field. */
    private String createField(String label, String type, boolean required) throws Exception {
        return json(asSuperAdmin(post(PLAN_FIELDS), """
                {"label": "%s", "type": "%s", "required": %b}
                """.formatted(label, type, required))
                .andExpect(status().isCreated())).get("key").asString();
    }

    private String fieldIdOf(String key) throws Exception {
        for (JsonNode field : json(asSuperAdmin(get(PLAN_FIELDS), null))) {
            if (field.get("key").asString().equals(key)) {
                return field.get("id").asString();
            }
        }
        throw new AssertionError("No plan field " + key);
    }

    private static String completePlan(String name, long version, String customKey, String customValue) {
        return """
                {"name": "%s", "price": "120.00", "currency": "USD", "periodicity": "MONTHLY",
                 "courtLimit": 10, "userLimit": 25, "customFields": {"%s": "%s"}, "version": %d}
                """.formatted(name, customKey, customValue, version);
    }

    /** Every basic field, at version 0. */
    private static String completePlanWithoutCustomFields(String name) {
        return """
                {"name": "%s", "price": "120.00", "currency": "USD", "periodicity": "MONTHLY",
                 "courtLimit": 10, "userLimit": 25, "version": 0}
                """.formatted(name);
    }

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static void setPlanOptions(String currencies, String periodicities) {
        TenantDatabase.admin().sql("""
                        UPDATE global.platform_parameter SET value = CAST(:value AS jsonb) WHERE key = :key
                        """)
                .param("value", currencies).param("key", "plan.currencies").update();
        TenantDatabase.admin().sql("""
                        UPDATE global.platform_parameter SET value = CAST(:value AS jsonb) WHERE key = :key
                        """)
                .param("value", periodicities).param("key", "plan.periodicities").update();
    }

    private ResultActions asSuperAdmin(MockHttpServletRequestBuilder request, String body) throws Exception {
        return as(superAdminToken, request, body);
    }

    private ResultActions as(String token, MockHttpServletRequestBuilder request, String body) throws Exception {
        MockHttpServletRequestBuilder withBody = body == null
                ? request.contentType(MediaType.APPLICATION_JSON)
                : request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(withBody.with(onHost(TestTenants.GLOBAL_HOST))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private static JsonNode json(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString());
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
}
