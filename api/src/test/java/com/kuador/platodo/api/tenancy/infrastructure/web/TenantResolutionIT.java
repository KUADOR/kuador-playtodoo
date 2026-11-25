package com.kuador.platodo.api.tenancy.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.application.port.out.TenantDomains;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

/**
 * Subdomain resolution of every api request with the real wiring and PostgreSQL. Only A and B have a schema: the
 * other complexes are registered without one, so any access to their schema would fail the request. Probe endpoints
 * count every request that reaches a handler. Numbers refer to the criteria of PLATODO-26.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantResolutionIT {

    private static final TenantId SUSPENDED = new TenantId("0d000000-0000-4000-8000-000000000001");
    private static final TenantId ARCHIVED = new TenantId("0d000000-0000-4000-8000-000000000002");
    private static final TenantId PURGED = new TenantId("0d000000-0000-4000-8000-000000000003");
    private static final TenantId PURGING = new TenantId("0d000000-0000-4000-8000-000000000004");
    private static final TenantId PROVISIONING = new TenantId("0d000000-0000-4000-8000-000000000005");

    private static final String RETIRED_SUBDOMAIN_OF_A = "retired-a";
    private static final String TENANT_PROBE = "/api/v1/test-probe/schema";
    private static final String GLOBAL_PROBE = "/api/v1/platform/test-probe";
    private static final String NO_SCOPE = "no tenant scope";

    @TestConfiguration(proxyBeanMethods = false)
    static class Probes {

        @Bean
        ProbeController probeController(DataSource tenantData) {
            return new ProbeController(JdbcClient.create(tenantData));
        }
    }

    /** A tenant operation that reads its schema and a global operation that reports the open scope. */
    @RestController
    static class ProbeController {

        private final JdbcClient tenantData;
        private final AtomicInteger calls = new AtomicInteger();

        ProbeController(JdbcClient tenantData) {
            this.tenantData = tenantData;
        }

        @GetMapping(TENANT_PROBE)
        String schema() {
            calls.incrementAndGet();
            return tenantData.sql("SELECT current_schema()").query(String.class).single();
        }

        @GetMapping(GLOBAL_PROBE)
        String scope() {
            calls.incrementAndGet();
            return TenantScope.current().map(TenantId::value).orElse(NO_SCOPE);
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ProbeController probe;
    @MockitoSpyBean
    private TenantDomains domains;

    @BeforeAll
    void registerComplexes(@Autowired TenantMigrations migrations) {
        TestTenants.provision(migrations, TestTenants.A);
        TestTenants.provision(migrations, TestTenants.B);
        TestTenants.register(SUSPENDED, "SUSPENDED");
        TestTenants.register(ARCHIVED, "ARCHIVED");
        TestTenants.register(PURGED, "PURGED");
        TestTenants.register(PURGING, "PURGING");
        TestTenants.register(PROVISIONING, "PROVISIONING");
        TenantDatabase.admin().sql("""
                        INSERT INTO global.tenant_domain (subdomain, tenant_id, is_current, created_at, retired_at)
                        VALUES (:subdomain, :id::uuid, false, now(), now())
                        ON CONFLICT (subdomain) DO NOTHING
                        """)
                .param("subdomain", RETIRED_SUBDOMAIN_OF_A)
                .param("id", TestTenants.A.value())
                .update();
    }

    @BeforeEach
    void resetProbe() {
        probe.calls.set(0);
    }

    @Test
    void theStateOfAnActiveComplexIsPublic_criterion1() throws Exception {
        mvc.perform(get("/api/v1/tenant").with(onHost(TestTenants.hostOf(TestTenants.A))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.displayName").value(TestTenants.DISPLAY_NAME));
    }

    @Test
    void eachRequestRunsOnlyInTheSchemaOfItsSubdomain_isolationAB() throws Exception {
        assertThat(probeSchemaOn(TestTenants.hostOf(TestTenants.A))).isEqualTo(TestTenants.schemaOf(TestTenants.A));
        assertThat(probeSchemaOn(TestTenants.hostOf(TestTenants.B))).isEqualTo(TestTenants.schemaOf(TestTenants.B));
        assertThat(probeSchemaOn(TestTenants.hostOf(TestTenants.A))).isEqualTo(TestTenants.schemaOf(TestTenants.A));
    }

    @Test
    void theSubdomainIsComparedInLowerCase() throws Exception {
        String upperCaseHost = TestTenants.hostOf(TestTenants.B).toUpperCase(java.util.Locale.ROOT);

        assertThat(probeSchemaOn(upperCaseHost)).isEqualTo(TestTenants.schemaOf(TestTenants.B));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "-abc", "abc-", "a--b", "xn--abc", "a_b", "%s"})
    void aMalformedSubdomainIsNotFoundBeforeAnyLookup_criterion2(String label) throws Exception {
        String host = label.formatted("a".repeat(64)) + "." + TestTenants.ROOT_DOMAIN;

        expectProblem(mvc.perform(get(TENANT_PROBE).with(onHost(host))), 404, "TENANT_NOT_FOUND");
        verifyNoInteractions(domains);
        assertThat(probe.calls).hasValue(0);
    }

    @Test
    void anUnknownSubdomainIsNotFound_criterion2() throws Exception {
        expectProblem(mvc.perform(get("/api/v1/tenant").with(onHost("nobody-here.localhost"))),
                404, "TENANT_NOT_FOUND");
        expectProblem(mvc.perform(get(TENANT_PROBE).with(onHost("nobody-here.localhost"))),
                404, "TENANT_NOT_FOUND");
        assertThat(probe.calls).hasValue(0);
    }

    @Test
    void aRetiredSubdomainNoLongerResolves() throws Exception {
        expectProblem(mvc.perform(get(TENANT_PROBE).with(onHost(RETIRED_SUBDOMAIN_OF_A + ".localhost"))),
                404, "TENANT_NOT_FOUND");
        assertThat(probe.calls).hasValue(0);
    }

    @Test
    void suspendedAndArchivedComplexesAreUnavailableToEveryone_criterion3() throws Exception {
        for (TenantId unavailable : new TenantId[] {SUSPENDED, ARCHIVED}) {
            String host = TestTenants.hostOf(unavailable);
            expectProblem(mvc.perform(get("/api/v1/tenant").with(onHost(host))), 403, "TENANT_UNAVAILABLE")
                    .andExpect(jsonPath("$.detail").value("The complex of this host is not available."));
            expectProblem(mvc.perform(get(TENANT_PROBE).with(onHost(host))), 403, "TENANT_UNAVAILABLE");
        }
        assertThat(probe.calls).hasValue(0);
    }

    @Test
    void purgedPurgingAndProvisioningComplexesAreNotFound() throws Exception {
        for (TenantId notOperating : new TenantId[] {PURGED, PURGING, PROVISIONING}) {
            expectProblem(mvc.perform(get(TENANT_PROBE).with(onHost(TestTenants.hostOf(notOperating)))),
                    404, "TENANT_NOT_FOUND");
        }
        assertThat(probe.calls).hasValue(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"localhost", "nested.test-x.localhost", "example.com"})
    void aHostOutsideTheRootDomainIsNotFound(String host) throws Exception {
        expectProblem(mvc.perform(get(TENANT_PROBE).with(onHost(host))), 404, "TENANT_NOT_FOUND");
        expectProblem(mvc.perform(get(GLOBAL_PROBE).with(onHost(host))), 404, "TENANT_NOT_FOUND");
        assertThat(probe.calls).hasValue(0);
    }

    @Test
    void tenantOperationsAreNotServedOnTheGlobalHost_criterion5() throws Exception {
        expectProblem(mvc.perform(get("/api/v1/tenant").with(onHost(TestTenants.GLOBAL_HOST))),
                404, "RESOURCE_NOT_FOUND");
        expectProblem(mvc.perform(get(TENANT_PROBE).with(onHost(TestTenants.GLOBAL_HOST))),
                404, "RESOURCE_NOT_FOUND");
        assertThat(probe.calls).hasValue(0);
    }

    @Test
    void platformOperationsAreNotServedOnATenantHost_criterion5() throws Exception {
        expectProblem(mvc.perform(get(GLOBAL_PROBE).with(onHost(TestTenants.hostOf(TestTenants.A)))),
                404, "RESOURCE_NOT_FOUND");
        assertThat(probe.calls).hasValue(0);
    }

    @Test
    void platformOperationsRunOnTheGlobalHostWithoutTenantScope() throws Exception {
        mvc.perform(get(GLOBAL_PROBE).with(onHost(TestTenants.GLOBAL_HOST)))
                .andExpect(status().isOk())
                .andExpect(content().string(NO_SCOPE));
    }

    private String probeSchemaOn(String host) throws Exception {
        return mvc.perform(get(TENANT_PROBE).with(onHost(host)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code));
    }
}
