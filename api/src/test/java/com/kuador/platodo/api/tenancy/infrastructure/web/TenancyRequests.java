package com.kuador.platodo.api.tenancy.infrastructure.web;

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

import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** HTTP calls of {@code contracts/tenancy.openapi.yaml} used by the lifecycle and subdomain integration tests. */
final class TenancyRequests {

    static final String PLATFORM_TENANTS = "/api/v1/platform/tenants";
    static final String TENANT_STATE = "/api/v1/tenant";
    static final String TENANT_SUBDOMAIN = "/api/v1/tenant/subdomain";
    static final String STAFF_LOGIN = "/api/v1/auth/staff/login";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvc mvc;

    TenancyRequests(MockMvc mvc) {
        this.mvc = mvc;
    }

    /** A subdomain no other test uses. */
    static String uniqueSubdomain(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static String hostOf(String subdomain) {
        return subdomain + "." + TestTenants.ROOT_DOMAIN;
    }

    static JsonNode json(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** Token of a new platform owner. */
    String platformOwnerToken() throws Exception {
        String email = TestAccounts.uniqueEmail("platform-owner");
        TestAccounts.platformOwner(email);
        return accessToken(login(TestTenants.GLOBAL_HOST, "/api/v1/auth/platform/login", email));
    }

    /** Token of a new player. */
    String playerToken() throws Exception {
        String email = TestAccounts.uniqueEmail("player");
        TestAccounts.player(email);
        return accessToken(login(TestTenants.GLOBAL_HOST, "/api/v1/auth/players/login", email));
    }

    /** Token of a new staff member with {@code role} of {@code tenant}, logged in on {@code subdomain}. */
    String staffToken(TenantId tenant, String subdomain, String role) throws Exception {
        String email = TestAccounts.uniqueEmail("staff");
        TestAccounts.staff(tenant, email, role);
        return accessToken(login(hostOf(subdomain), STAFF_LOGIN, email));
    }

    ResultActions register(String token, String displayName, String subdomain, String ownerEmail) throws Exception {
        return mvc.perform(withToken(post(PLATFORM_TENANTS).with(onHost(TestTenants.GLOBAL_HOST)), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"displayName": "%s", "subdomain": "%s", "ownerEmail": "%s"}
                        """.formatted(displayName, subdomain, ownerEmail)));
    }

    /** Registers an active complex on {@code subdomain} and returns its id. */
    TenantId registered(String token, String subdomain) throws Exception {
        JsonNode body = json(register(token, "Complex " + subdomain, subdomain, TestAccounts.uniqueEmail("owner"))
                .andExpect(status().isCreated()));
        return new TenantId(body.get("id").asString());
    }

    ResultActions lifecycle(String token, TenantId tenant, String operation) throws Exception {
        return mvc.perform(withToken(
                post(PLATFORM_TENANTS + "/" + tenant.value() + "/" + operation).with(onHost(TestTenants.GLOBAL_HOST)),
                token));
    }

    ResultActions changeRetention(String token, TenantId tenant, String body) throws Exception {
        return mvc.perform(withToken(
                put(PLATFORM_TENANTS + "/" + tenant.value() + "/retention").with(onHost(TestTenants.GLOBAL_HOST)),
                token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    ResultActions changeSubdomain(String token, String onSubdomain, String subdomain) throws Exception {
        return mvc.perform(withToken(put(TENANT_SUBDOMAIN).with(onHost(hostOf(onSubdomain))), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"subdomain": "%s"}
                        """.formatted(subdomain)));
    }

    ResultActions availability(String subdomain) throws Exception {
        return mvc.perform(get("/api/v1/subdomains/" + subdomain + "/availability")
                .with(onHost(TestTenants.GLOBAL_HOST)));
    }

    ResultActions suggestion(String name) throws Exception {
        return mvc.perform(get("/api/v1/subdomains/suggestion").with(onHost(TestTenants.GLOBAL_HOST))
                .param("name", name));
    }

    ResultActions tenantState(String subdomain) throws Exception {
        return mvc.perform(get(TENANT_STATE).with(onHost(hostOf(subdomain))));
    }

    ResultActions staffLogin(String subdomain, String email) throws Exception {
        return mvc.perform(loginRequest(hostOf(subdomain), STAFF_LOGIN, email));
    }

    private String accessToken(ResultActions login) throws Exception {
        return json(login.andExpect(status().isOk())).get("accessToken").asString();
    }

    private ResultActions login(String host, String path, String email) throws Exception {
        return mvc.perform(loginRequest(host, path, email));
    }

    private static MockHttpServletRequestBuilder loginRequest(String host, String path, String email) {
        return post(path).with(onHost(host))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, TestAccounts.PASSWORD));
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
