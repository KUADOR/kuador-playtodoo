package com.kuador.platodo.api.subscriptions.infrastructure.web;

import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.json;
import static com.kuador.platodo.api.subscriptions.infrastructure.web.ComplexRequests.uniqueSubdomain;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.tenancy.application.port.out.TenantSchemas;

/**
 * A registration whose schema cannot be created (ADR-014): it is kept in {@code PROVISIONING_FAILED} with its
 * subdomain and subscription, the complex host is not served, and a retry finishes it. The schema creation fails once
 * through a spy on the production {@link TenantSchemas}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class ComplexProvisioningRetryIT {

    @Autowired
    private MockMvc mvc;
    @MockitoSpyBean
    private TenantSchemas schemas;

    private ComplexRequests api;
    private String superAdminToken;
    private String planId;

    @BeforeEach
    void setUp() throws Exception {
        api = new ComplexRequests(mvc);
        ComplexRequests.allowTestPlanOptions();
        superAdminToken = api.superAdminToken();
        planId = api.publishedPlan(superAdminToken, "Retry plan " + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        ComplexRequests.restorePlanOptions();
    }

    @Test
    void aFailedProvisioningIsKeptAndRetriedUntilTheComplexIsActive() throws Exception {
        doThrow(new IllegalStateException("Simulated schema failure")).doCallRealMethod()
                .when(schemas).provision(any());
        String subdomain = uniqueSubdomain("retry");
        String ownerEmail = TestAccounts.uniqueEmail("retry-owner");

        String complexId = json(api.register(superAdminToken, "Retry complex", subdomain, ownerEmail, planId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantStatus").value("PROVISIONING_FAILED"))
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.allowedTransitions").isEmpty())
                .andExpect(jsonPath("$.version").value(1))).get("id").asString();

        api.detail(superAdminToken, complexId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantStatus").value("PROVISIONING_FAILED"));
        api.tenantState(subdomain).andExpect(status().isNotFound());
        api.availability(subdomain).andExpect(jsonPath("$.reason").value("TAKEN"));

        api.retry(superAdminToken, complexId)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.tenantStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.ownerEmail").value(ownerEmail))
                .andExpect(jsonPath("$.allowedTransitions.length()").value(3))
                .andExpect(jsonPath("$.version").value(3));
        api.tenantState(subdomain).andExpect(status().isOk());
        api.retry(superAdminToken, complexId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_PROVISIONING_FAILED"));
    }

    @Test
    void aRetryThatFailsAgainWaitsForAnotherOne() throws Exception {
        doThrow(new IllegalStateException("Simulated schema failure"))
                .doThrow(new IllegalStateException("Simulated schema failure"))
                .doCallRealMethod()
                .when(schemas).provision(any());
        String complexId = json(api.register(superAdminToken, "Retry twice", uniqueSubdomain("retry-twice"),
                TestAccounts.uniqueEmail("retry-owner"), planId).andExpect(status().isCreated())).get("id").asString();

        api.retry(superAdminToken, complexId)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.tenantStatus").value("PROVISIONING_FAILED"));
        api.retry(superAdminToken, complexId)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.tenantStatus").value("ACTIVE"));
    }
}
