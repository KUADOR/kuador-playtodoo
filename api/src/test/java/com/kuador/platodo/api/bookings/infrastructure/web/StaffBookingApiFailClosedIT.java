package com.kuador.platodo.api.bookings.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.kuador.platodo.api.bookings.application.CreatePendingBooking;
import com.kuador.platodo.api.support.TenantDatabase;

/**
 * With only the staff API enabled and no Identity or Courts adapters registered, the default adapters reject
 * every request instead of acting without an authenticated staff member, and no player endpoint exists.
 */
@SpringBootTest(properties = ConditionalOnStaffApiEnabled.PROPERTY + "=true")
@AutoConfigureMockMvc
class StaffBookingApiFailClosedIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantOnly {

        @Bean
        DataSource tenantDataSource() {
            return TenantDatabase.migratedTenant("tenant_staff_api_fail_closed");
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
