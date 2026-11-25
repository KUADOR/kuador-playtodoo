package com.kuador.platodo.api.bookings.infrastructure.web;

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
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.kuador.platodo.api.support.TenantDatabase;

/**
 * With the player API enabled but no Identity or Courts adapters registered, the default adapters reject
 * every request instead of writing without an authenticated player or an invented slot duration.
 */
@SpringBootTest(properties = ConditionalOnPlayerApiEnabled.PROPERTY + "=true")
@AutoConfigureMockMvc
class PlayerBookingApiFailClosedIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantOnly {

        @Bean
        DataSource tenantDataSource() {
            return TenantDatabase.migratedTenant("tenant_player_api_fail_closed");
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
