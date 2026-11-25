package com.kuador.platodo.api.identity.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnPlayerApiEnabled;
import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnStaffApiEnabled;
import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import tools.jackson.databind.json.JsonMapper;

/**
 * A player has one global account that works on every complex, while the data a player creates in a complex stays in
 * that complex's schema (PLATODO-26 criterion 9). Real tenant resolution, token filter, PostgreSQL and the player
 * booking endpoints.
 */
@SpringBootTest(properties = {
        ConditionalOnPlayerApiEnabled.PROPERTY + "=true",
        ConditionalOnStaffApiEnabled.PROPERTY + "=true"})
@Import(TwoComplexesWithBookingApis.class)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class PlayerGlobalAccountIT {

    private static final TenantId A = TestTenants.A;
    private static final TenantId B = TestTenants.B;
    private static final String FUTURE_SLOT = "2030-01-01T20:00:00Z";
    private static final String PLAYER_LOGIN = "/api/v1/auth/players/login";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StubCourtSlotRules courts;

    private String email;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        email = TestAccounts.uniqueEmail("player");
        playerId = TestAccounts.player(email);
    }

    @Test
    void oneGlobalAccountBooksInBothComplexesAndEachBookingStaysInItsSchema() throws Exception {
        String token = playerToken();

        String inA = bookingId(book(A, token).andExpect(status().isCreated()));
        String inB = bookingId(book(B, token).andExpect(status().isCreated()));

        assertThat(playerBookingsIn(A)).containsExactly(inA);
        assertThat(playerBookingsIn(B)).containsExactly(inB);
        mvc.perform(get("/api/v1/bookings/" + inB).with(onHost(TestTenants.hostOf(A))).header(
                        HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void theAccountLivesOnlyInTheGlobalSchema() throws Exception {
        book(A, playerToken()).andExpect(status().isCreated());

        assertThat(TenantDatabase.admin().sql("""
                        SELECT count(*) FROM global.player_account WHERE id = :id AND email = :email
                        """)
                .param("id", playerId).param("email", email)
                .query(Long.class).single()).isOne();
        assertThat(TenantDatabase.admin().sql("""
                        SELECT table_schema || '.' || table_name FROM information_schema.columns
                        WHERE table_schema IN (:schemas) AND column_name IN ('email', 'password_hash')
                          AND table_name <> 'staff_user'
                        """)
                .param("schemas", List.of(TestTenants.schemaOf(A), TestTenants.schemaOf(B)))
                .query(String.class).list())
                .as("tenant tables that could hold a copy of the player's account")
                .isEmpty();
    }

    @Test
    void theAccountIsCheckedInTheGlobalSchemaOnEveryRequest() throws Exception {
        String token = playerToken();
        TestAccounts.disablePlayer(playerId);

        book(A, token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        login(email).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void playerLoginIsServedOnlyOnTheGlobalHost() throws Exception {
        mvc.perform(post(PLAYER_LOGIN).with(onHost(TestTenants.hostOf(A)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void theEmailIsMatchedCaseInsensitively() throws Exception {
        login("  " + email.toUpperCase() + " ").andExpect(status().isOk());
    }

    private String playerToken() throws Exception {
        String response = login(email).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("accessToken").asString();
    }

    private ResultActions login(String loginEmail) throws Exception {
        return mvc.perform(post(PLAYER_LOGIN).with(onHost(TestTenants.GLOBAL_HOST))
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(loginEmail)));
    }

    private static String credentials(String loginEmail) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(loginEmail, TestAccounts.PASSWORD);
    }

    private ResultActions book(TenantId tenant, String token) throws Exception {
        return mvc.perform(post("/api/v1/bookings").with(onHost(TestTenants.hostOf(tenant)))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"courtId": "%s", "start": "%s"}
                        """.formatted(courts.addCourt(Duration.ofMinutes(60)), FUTURE_SLOT)));
    }

    private List<String> playerBookingsIn(TenantId tenant) {
        return TenantDatabase.admin()
                .sql("SELECT id::text FROM \"%s\".booking WHERE player_id = :player"
                        .formatted(TestTenants.schemaOf(tenant)))
                .param("player", playerId)
                .query(String.class)
                .list();
    }

    private static String bookingId(ResultActions created) throws Exception {
        return JSON.readTree(created.andReturn().getResponse().getContentAsString()).get("id").asString();
    }
}
