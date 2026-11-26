package com.kuador.platodo.api.subscriptions.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * HTTP calls of the tag «Complexes» of {@code contracts/platform-backoffice.openapi.yaml} for the integration tests,
 * plus the plans they need. The plan options are global: {@link #allowTestPlanOptions()} sets them and
 * {@link #restorePlanOptions()} puts back the empty values of the migration.
 */
final class ComplexRequests {

    static final String COMPLEXES = "/api/v1/platform/complexes";
    static final String PLATFORM_LOGIN = "/api/v1/auth/platform/login";
    static final String PLAYER_LOGIN = "/api/v1/auth/players/login";

    private static final String PLANS = "/api/v1/platform/plans";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvc mvc;

    ComplexRequests(MockMvc mvc) {
        this.mvc = mvc;
    }

    static String uniqueSubdomain(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static JsonNode json(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString());
    }

    static void allowTestPlanOptions() {
        setPlanOption("plan.currencies", "[\"USD\"]");
        setPlanOption("plan.periodicities", "[\"MONTHLY\"]");
    }

    static void restorePlanOptions() {
        setPlanOption("plan.currencies", "[]");
        setPlanOption("plan.periodicities", "[]");
    }

    String superAdminToken() throws Exception {
        String email = TestAccounts.uniqueEmail("complexes-super-admin");
        TestAccounts.platformOwner(email);
        return logIn(PLATFORM_LOGIN, email);
    }

    String operatorToken() throws Exception {
        String email = TestAccounts.uniqueEmail("complexes-operator");
        TestAccounts.operator(email);
        return logIn(PLATFORM_LOGIN, email);
    }

    String playerToken() throws Exception {
        String email = TestAccounts.uniqueEmail("complexes-player");
        TestAccounts.player(email);
        return logIn(PLAYER_LOGIN, email);
    }

    /** A published plan with every basic field and no custom fields; returns its id. */
    String publishedPlan(String superAdminToken, String name) throws Exception {
        String planId = draftPlan(superAdminToken, name);
        as(superAdminToken, put(PLANS + "/" + planId), """
                {"name": "%s", "price": "120.00", "currency": "USD", "periodicity": "MONTHLY",
                 "courtLimit": 10, "userLimit": 25, "version": 0}
                """.formatted(name)).andExpect(status().isOk());
        as(superAdminToken, post(PLANS + "/" + planId + "/publication"), null).andExpect(status().isOk());
        return planId;
    }

    String draftPlan(String superAdminToken, String name) throws Exception {
        return json(as(superAdminToken, post(PLANS), "{\"name\": \"%s\"}".formatted(name))
                .andExpect(status().isCreated())).get("id").asString();
    }

    ResultActions register(String token, String displayName, String subdomain, String ownerEmail, String planId)
            throws Exception {
        return as(token, post(COMPLEXES), """
                {"displayName": "%s", "subdomain": "%s", "ownerEmail": "%s", "planId": "%s"}
                """.formatted(displayName, subdomain, ownerEmail, planId));
    }

    ResultActions detail(String token, String complexId) throws Exception {
        return as(token, get(COMPLEXES + "/" + complexId), null);
    }

    ResultActions list(String token, int page, int size) throws Exception {
        return as(token, get(COMPLEXES).param("page", String.valueOf(page)).param("size", String.valueOf(size)),
                null);
    }

    ResultActions retry(String token, String complexId) throws Exception {
        return as(token, post(COMPLEXES + "/" + complexId + "/provisioning-retries"), null);
    }

    ResultActions availability(String subdomain) throws Exception {
        return mvc.perform(get("/api/v1/subdomains/" + subdomain + "/availability")
                .with(onHost(TestTenants.GLOBAL_HOST)));
    }

    /** {@code GET /api/v1/tenant} on the host of {@code subdomain}: 200 only while the complex operates. */
    ResultActions tenantState(String subdomain) throws Exception {
        return mvc.perform(get("/api/v1/tenant").with(onHost(subdomain + "." + TestTenants.ROOT_DOMAIN)));
    }

    ResultActions as(String token, MockHttpServletRequestBuilder request, String body) throws Exception {
        MockHttpServletRequestBuilder withBody = body == null
                ? request.contentType(MediaType.APPLICATION_JSON)
                : request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(withBody.with(onHost(TestTenants.GLOBAL_HOST))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String logIn(String path, String email) throws Exception {
        return json(mvc.perform(post(path).with(onHost(TestTenants.GLOBAL_HOST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, TestAccounts.PASSWORD)))
                .andExpect(status().isOk()))
                .get("accessToken").asString();
    }

    private static void setPlanOption(String key, String value) {
        TenantDatabase.admin().sql("UPDATE global.platform_parameter SET value = CAST(:value AS jsonb) WHERE key = :key")
                .param("value", value).param("key", key).update();
    }
}
