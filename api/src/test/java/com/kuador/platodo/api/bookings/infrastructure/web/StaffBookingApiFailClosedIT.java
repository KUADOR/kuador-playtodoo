package com.kuador.platodo.api.bookings.infrastructure.web;

import static com.kuador.platodo.api.support.TestTenants.FLYWAY_INITIALIZER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.kuador.platodo.api.bookings.application.CreatePendingBooking;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TenantHostRequests;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

/**
 * With only the staff API enabled and no Courts adapter registered and no access token, the default adapters reject
 * every request instead of acting without an authenticated staff member, and no player endpoint exists.
 */
@SpringBootTest(properties = ConditionalOnStaffApiEnabled.PROPERTY + "=true")
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class StaffBookingApiFailClosedIT {

    private static final TenantId TENANT = new TenantId("0c000000-0000-4000-8000-000000000004");

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantOnly {

        /** Every request goes to the host of this class's tenant and is resolved by Tenancy. */
        @Bean
        @DependsOn(FLYWAY_INITIALIZER)
        TenantHostRequests tenantHost(TenantMigrations migrations) {
            return new TenantHostRequests(TestTenants.provision(migrations, TENANT));
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ApplicationContext context;

    @Test
    void blockingWithoutIdentityIsUnauthenticated() throws Exception {
        mvc.perform(post("/api/v1/staff/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courtId": "%s", "start": "2030-01-01T20:00:00Z"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void listingWithoutIdentityIsUnauthenticated() throws Exception {
        mvc.perform(get("/api/v1/staff/bookings")
                        .param("from", "2030-01-01T00:00:00Z")
                        .param("to", "2030-01-02T00:00:00Z"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void thePlayerApiStaysOff() {
        assertThat(context.getBeanNamesForType(CreatePendingBooking.class)).isEmpty();
    }
}
