package com.kuador.platodo.api.bookings.infrastructure.web;

import static com.kuador.platodo.api.support.TestTenants.FLYWAY_INITIALIZER;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.kuador.platodo.api.support.FixedTenantScopeFilter;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

/**
 * With the player API enabled but no Identity or Courts adapters registered, the default adapters reject
 * every request instead of writing without an authenticated player or an invented slot duration.
 */
@SpringBootTest(properties = ConditionalOnPlayerApiEnabled.PROPERTY + "=true")
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class PlayerBookingApiFailClosedIT {

    private static final TenantId TENANT = new TenantId("0c000000-0000-4000-8000-000000000002");

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantOnly {

        /** Stand-in for the subdomain resolution of PLATODO-50: every request runs in this class's tenant. */
        @Bean
        @DependsOn(FLYWAY_INITIALIZER)
        FixedTenantScopeFilter tenantScope(TenantMigrations migrations) {
            return new FixedTenantScopeFilter(TestTenants.provision(migrations, TENANT));
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void creatingABookingWithoutIdentityIsUnauthenticated() throws Exception {
        mvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courtId": "%s", "start": "2030-01-01T20:00:00Z"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void readingOccupancyWithoutIdentityIsUnauthenticated() throws Exception {
        mvc.perform(get("/api/v1/courts/{courtId}/occupancy", UUID.randomUUID())
                        .param("from", "2030-01-01T00:00:00Z")
                        .param("to", "2030-01-02T00:00:00Z"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
