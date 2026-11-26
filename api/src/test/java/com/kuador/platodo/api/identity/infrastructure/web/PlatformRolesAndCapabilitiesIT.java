package com.kuador.platodo.api.identity.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static com.kuador.platodo.api.support.TestTenants.FLYWAY_INITIALIZER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Roles SUPER_ADMIN and OPERATOR with capabilities (ADR-017; PLATODO-27 CA15, CA16 and CA18), over HTTP with the real
 * tenant resolution, token filter and PostgreSQL. A probe operation guarded by {@code SUBSCRIPTION_PAYMENT_RECORD}
 * stands in for the capability-guarded operations that later tasks add. Operator capabilities are global, so every
 * test reads the current version first and {@link #disableEveryCapability()} restores the initial state.
 */
@SpringBootTest
@Import(PlatformRolesAndCapabilitiesIT.GuardedProbe.class)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class PlatformRolesAndCapabilitiesIT {

    private static final String ME = "/api/v1/platform/me";
    private static final String CAPABILITIES = "/api/v1/platform/operator-capabilities";
    private static final String CHANGES = CAPABILITIES + "/changes";
    private static final String PLATFORM_TENANTS = "/api/v1/platform/tenants";
    private static final String GUARDED_PROBE = "/api/v1/platform/test-probe/payments";
    private static final String PLATFORM_LOGIN = "/api/v1/auth/platform/login";
    private static final String PLAYER_LOGIN = "/api/v1/auth/players/login";
    private static final String PAYMENTS = Capability.SUBSCRIPTION_PAYMENT_RECORD.name();
    private static final TenantId A = TestTenants.A;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccessTokens accessTokens;

    private UUID superAdminId;
    private String superAdminToken;
    private String operatorToken;

    @BeforeEach
    void setUp() throws Exception {
        String superAdminEmail = TestAccounts.uniqueEmail("super-admin");
        superAdminId = TestAccounts.platformOwner(superAdminEmail);
        superAdminToken = logIn(PLATFORM_LOGIN, superAdminEmail);
        String operatorEmail = TestAccounts.uniqueEmail("operator");
        TestAccounts.operator(operatorEmail);
        operatorToken = logIn(PLATFORM_LOGIN, operatorEmail);
    }

    @AfterEach
    void disableEveryCapability() throws Exception {
        String changes = JSON.writeValueAsString(List.of(Capability.values()).stream()
                .map(capability -> Map.of("key", capability.name(), "enabled", false))
                .toList());
        changeCapabilities(superAdminToken, currentVersion(), changes).andExpect(status().isOk());
    }

    /** CA15: the SUPER_ADMIN sees every capability; an operator starts with none. */
    @Test
    void meReturnsTheRoleAndTheEffectiveCapabilities() throws Exception {
        onGlobalHost(get(ME), superAdminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(superAdminId.toString()))
                .andExpect(jsonPath("$.displayName").value(TestAccounts.PLATFORM_USER_NAME))
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.capabilities").value(contains(
                        "COMPLEX_CREATE", "SUBSCRIPTION_PAYMENT_RECORD", "SUBSCRIPTION_STATUS_CHANGE",
                        "TENANT_DATA_ACCESS")));
        onGlobalHost(get(ME), operatorToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.capabilities").value(empty()));
    }

    /** CA16: an operator without the capability gets 403; enabling and disabling apply on the next request. */
    @Test
    void aCapabilityChangeAppliesOnTheOperatorsNextRequest() throws Exception {
        onGlobalHost(post(GUARDED_PROBE), operatorToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CAPABILITY_REVOKED"));

        long version = currentVersion();
        changeCapabilities(superAdminToken, version, toggle(PAYMENTS, true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(version + 1))
                .andExpect(jsonPath("$.items[?(@.key == 'SUBSCRIPTION_PAYMENT_RECORD')].enabled")
                        .value(contains(true)));

        onGlobalHost(post(GUARDED_PROBE), operatorToken).andExpect(status().isNoContent());
        onGlobalHost(get(ME), operatorToken).andExpect(jsonPath("$.capabilities").value(contains(PAYMENTS)));

        changeCapabilities(superAdminToken, version + 1, toggle(PAYMENTS, false)).andExpect(status().isOk());

        onGlobalHost(post(GUARDED_PROBE), operatorToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CAPABILITY_REVOKED"));
    }

    /** CA16: each effective change is recorded with who made it, newest first. */
    @Test
    void everyEffectiveChangeIsRecordedWithWhoMadeIt() throws Exception {
        long version = currentVersion();
        changeCapabilities(superAdminToken, version, toggle(PAYMENTS, true)).andExpect(status().isOk());
        changeCapabilities(superAdminToken, version + 1, toggle(PAYMENTS, false)).andExpect(status().isOk());

        JsonNode page = JSON.readTree(onGlobalHost(get(CHANGES).param("size", "2"), superAdminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andReturn().getResponse().getContentAsString());
        JsonNode newest = page.get("items").get(0);
        JsonNode previous = page.get("items").get(1);
        assertThat(newest.get("key").asString()).isEqualTo(PAYMENTS);
        assertThat(newest.get("enabled").asBoolean()).isFalse();
        assertThat(previous.get("enabled").asBoolean()).isTrue();
        assertThat(newest.get("changedBy").get("id").asString()).isEqualTo(superAdminId.toString());
        assertThat(newest.get("changedBy").get("displayName").asString()).isEqualTo(TestAccounts.PLATFORM_USER_NAME);
        assertThat(page.get("totalItems").asLong()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void aStaleVersionAnswersConflictAndChangesNothing() throws Exception {
        long version = currentVersion();
        changeCapabilities(superAdminToken, version, toggle(PAYMENTS, true)).andExpect(status().isOk());

        changeCapabilities(superAdminToken, version, toggle(Capability.COMPLEX_CREATE.name(), true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_STATE"));

        onGlobalHost(get(CAPABILITIES), superAdminToken)
                .andExpect(jsonPath("$.version").value(version + 1))
                .andExpect(jsonPath("$.items[?(@.key == 'COMPLEX_CREATE')].enabled").value(contains(false)));
    }

    /** Two SUPER_ADMIN changes from the same version: exactly one applies, the other is stale. */
    @Test
    void concurrentChangesFromTheSameVersionApplyOnlyOnce() throws Exception {
        long version = currentVersion();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> enablePayments = () -> {
            start.await();
            return changeCapabilities(superAdminToken, version, toggle(PAYMENTS, true))
                    .andReturn().getResponse().getStatus();
        };
        Callable<Integer> enableComplexes = () -> {
            start.await();
            return changeCapabilities(superAdminToken, version, toggle(Capability.COMPLEX_CREATE.name(), true))
                    .andReturn().getResponse().getStatus();
        };
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(enablePayments);
            Future<Integer> second = executor.submit(enableComplexes);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(currentVersion()).isEqualTo(version + 1);
    }

    /** ADR-017: capabilities, their change log and the lifecycle of PLATODO-26 are SUPER_ADMIN only. */
    @Test
    void anOperatorCannotReachSuperAdminOperations() throws Exception {
        onGlobalHost(get(CAPABILITIES), operatorToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        changeCapabilities(operatorToken, currentVersion(), toggle(PAYMENTS, true))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        onGlobalHost(get(CHANGES), operatorToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        onGlobalHost(post(PLATFORM_TENANTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"displayName": "Operator complex", "subdomain": "operator-complex",
                         "ownerEmail": "o@example.com"}
                        """), operatorToken)
                .andExpect(status().isForbidden());
    }

    /** CA18: players and staff of a complex never reach the platform operations. */
    @Test
    void playersAndComplexStaffDoNotReachThePlatformOperations() throws Exception {
        String playerEmail = TestAccounts.uniqueEmail("player");
        TestAccounts.player(playerEmail);
        String playerToken = logIn(PLAYER_LOGIN, playerEmail);
        UUID staffId = TestAccounts.staff(A, TestAccounts.uniqueEmail("staff"), "OWNER");
        String staffToken = accessTokens.issue(AuthenticatedPrincipal.staff(staffId, A, "OWNER")).value();

        for (MockHttpServletRequestBuilder request : List.of(get(ME), get(CAPABILITIES), get(CHANGES),
                post(GUARDED_PROBE))) {
            onGlobalHost(request, playerToken)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        changeCapabilities(playerToken, currentVersion(), toggle(PAYMENTS, true))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        // A staff token is only accepted on the host of its own complex (PLATODO-26), and platform operations are
        // not served there.
        onGlobalHost(get(ME), staffToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get(ME).with(onHost(TestTenants.hostOf(A))).header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void withoutATokenEveryPlatformOperationIsUnauthenticated() throws Exception {
        for (MockHttpServletRequestBuilder request : List.of(get(ME), get(CAPABILITIES), get(CHANGES),
                post(GUARDED_PROBE))) {
            mvc.perform(request.with(onHost(TestTenants.GLOBAL_HOST)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
    }

    @Test
    void malformedChangesAndPagesAreInvalidRequests() throws Exception {
        long version = currentVersion();
        changeCapabilities(superAdminToken, version, toggle("UNKNOWN_CAPABILITY", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        changeCapabilities(superAdminToken, version, "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        onGlobalHost(put(CAPABILITIES).contentType(MediaType.APPLICATION_JSON).content("""
                        {"changes": [{"key": "COMPLEX_CREATE", "enabled": true}]}
                        """), superAdminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        onGlobalHost(get(CHANGES).param("size", "101"), superAdminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(currentVersion()).isEqualTo(version);
    }

    private long currentVersion() throws Exception {
        String body = onGlobalHost(get(CAPABILITIES), superAdminToken)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body).get("version").asLong();
    }

    private ResultActions changeCapabilities(String token, long version, String changesJson) throws Exception {
        return onGlobalHost(put(CAPABILITIES).contentType(MediaType.APPLICATION_JSON).content("""
                {"version": %d, "changes": %s}
                """.formatted(version, changesJson)), token);
    }

    private static String toggle(String key, boolean enabled) {
        return JSON.writeValueAsString(List.of(Map.of("key", key, "enabled", enabled)));
    }

    private ResultActions onGlobalHost(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.with(onHost(TestTenants.GLOBAL_HOST))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
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

    /** Tenant A provisioned, plus a platform operation guarded by {@code SUBSCRIPTION_PAYMENT_RECORD}. */
    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class GuardedProbe {

        private final CapabilityGuard guard;

        GuardedProbe(CapabilityGuard guard) {
            this.guard = guard;
        }

        @Bean
        @DependsOn(FLYWAY_INITIALIZER)
        TenantId provisionedTenant(TenantMigrations migrations) {
            return TestTenants.provision(migrations, A);
        }

        @PostMapping(GUARDED_PROBE)
        ResponseEntity<Void> recordPayment() {
            guard.require(Capability.SUBSCRIPTION_PAYMENT_RECORD);
            return ResponseEntity.noContent().build();
        }
    }
}
