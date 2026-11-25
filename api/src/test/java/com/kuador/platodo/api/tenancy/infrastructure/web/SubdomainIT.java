package com.kuador.platodo.api.tenancy.infrastructure.web;

import static com.kuador.platodo.api.support.TenantHostRequests.onHost;
import static com.kuador.platodo.api.tenancy.infrastructure.web.TenancyRequests.uniqueSubdomain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.event.TenantSubdomainChanged;

/**
 * Subdomain suggestion, availability and change by the owner (ADR-012), over HTTP with the real wiring and
 * PostgreSQL. Numbers refer to the criteria of PLATODO-26.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
@RecordApplicationEvents
class SubdomainIT {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String LOCAL_TENANT_URL = "http://%s.localhost:8000";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ApplicationEvents events;

    private TenancyRequests api;
    private String platformToken;

    @BeforeEach
    void setUp() throws Exception {
        api = new TenancyRequests(mvc);
        platformToken = api.platformOwnerToken();
    }

    /** Criterion 14: the system suggests a free subdomain from the name; a taken one gets the next number. */
    @Test
    void theSuggestionComesFromTheNameAndSkipsTakenSubdomains() throws Exception {
        String token = UUID.randomUUID().toString().substring(0, 8);
        String name = "Canchas Él Sol " + token;
        String base = "canchas-el-sol-" + token;

        api.suggestion(name).andExpect(status().isOk()).andExpect(jsonPath("$.subdomain").value(base));
        api.registered(platformToken, base);
        api.suggestion(name).andExpect(jsonPath("$.subdomain").value(base + "-2"));
    }

    @Test
    void theSuggestionNeedsAName() throws Exception {
        expectProblem(mvc.perform(get("/api/v1/subdomains/suggestion").with(onHost("api.localhost"))), 400,
                "INVALID_REQUEST");
        expectProblem(api.suggestion(" "), 400, "INVALID_REQUEST");
        expectProblem(api.suggestion("x".repeat(121)), 400, "INVALID_REQUEST");
    }

    @Test
    void availabilityChecksFormatReservedListAndUse() throws Exception {
        String used = uniqueSubdomain("used");
        api.registered(platformToken, used);

        expectAvailability(api.availability(used.toUpperCase()), used, false, "TAKEN");
        expectAvailability(api.availability("Admin"), "admin", false, "RESERVED");
        expectAvailability(api.availability("a--b"), "a--b", false, "INVALID");
        String free = uniqueSubdomain("free");
        expectAvailability(api.availability(free), free, true, null);
        expectProblem(api.availability("a".repeat(256)), 400, "INVALID_REQUEST");
    }

    /**
     * Criterion 14: the owner changes the subdomain after creation; the new one resolves to the same complex, the
     * previous one is not found and stays retired for everyone.
     */
    @Test
    void theOwnerChangesTheSubdomainAndThePreviousOneIsRetired() throws Exception {
        String previous = uniqueSubdomain("old");
        TenantId id = api.registered(platformToken, previous);
        String owner = api.staffToken(id, previous, "OWNER");
        String next = uniqueSubdomain("new");

        api.changeSubdomain(owner, previous, next.toUpperCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subdomain").value(next))
                .andExpect(jsonPath("$.url").value(LOCAL_TENANT_URL.formatted(next)));

        api.tenantState(next).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Complex " + previous));
        expectProblem(api.tenantState(previous), 404, "TENANT_NOT_FOUND");
        expectAvailability(api.availability(previous), previous, false, "TAKEN");
        expectProblem(api.register(platformToken, "Impostor", previous, "impostor@example.com"), 409,
                "SUBDOMAIN_TAKEN");
        assertThat(events.stream(TenantSubdomainChanged.class)).singleElement().satisfies(changed -> {
            assertThat(changed.tenantId()).isEqualTo(id);
            assertThat(changed.previousSubdomain()).isEqualTo(previous);
            assertThat(changed.subdomain()).isEqualTo(next);
        });
    }

    /** The token is bound to the complex, not to its subdomain: the owner keeps working on the new host. */
    @Test
    void theOwnerTokenKeepsWorkingOnTheNewSubdomain() throws Exception {
        String first = uniqueSubdomain("first");
        TenantId id = api.registered(platformToken, first);
        String owner = api.staffToken(id, first, "OWNER");
        String second = uniqueSubdomain("second");
        api.changeSubdomain(owner, first, second).andExpect(status().isOk());

        api.changeSubdomain(owner, second, first.replace("first", "third")).andExpect(status().isOk());

        expectProblem(api.changeSubdomain(owner, second, uniqueSubdomain("x")), 404, "TENANT_NOT_FOUND");
    }

    @Test
    void settingTheCurrentSubdomainAgainChangesNothing() throws Exception {
        String subdomain = uniqueSubdomain("same");
        TenantId id = api.registered(platformToken, subdomain);
        String owner = api.staffToken(id, subdomain, "OWNER");

        api.changeSubdomain(owner, subdomain, subdomain)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subdomain").value(subdomain));

        assertThat(events.stream(TenantSubdomainChanged.class)).isEmpty();
        assertThat(subdomainsOf(id)).containsExactly(subdomain);
    }

    @Test
    void onlyTheOwnerOfTheComplexChangesItsSubdomain() throws Exception {
        String subdomain = uniqueSubdomain("perm");
        TenantId id = api.registered(platformToken, subdomain);
        String target = uniqueSubdomain("target");

        expectProblem(api.changeSubdomain(null, subdomain, target), 401, "UNAUTHENTICATED");
        expectProblem(api.changeSubdomain(api.staffToken(id, subdomain, "ADMIN"), subdomain, target), 403,
                "FORBIDDEN");
        expectProblem(api.changeSubdomain(api.playerToken(), subdomain, target), 403, "FORBIDDEN");

        assertThat(subdomainsOf(id)).containsExactly(subdomain);
    }

    /** Isolation: the owner of A cannot change the subdomain of B, not even from the host of B. */
    @Test
    void theOwnerOfOneComplexCannotChangeAnotherOne() throws Exception {
        String subdomainOfA = uniqueSubdomain("own-a");
        String subdomainOfB = uniqueSubdomain("own-b");
        TenantId a = api.registered(platformToken, subdomainOfA);
        TenantId b = api.registered(platformToken, subdomainOfB);
        String ownerOfA = api.staffToken(a, subdomainOfA, "OWNER");

        expectProblem(api.changeSubdomain(ownerOfA, subdomainOfB, uniqueSubdomain("hijack")), 401,
                "UNAUTHENTICATED");

        assertThat(subdomainsOf(b)).containsExactly(subdomainOfB);
    }

    @Test
    void theSubdomainRulesApplyToTheChange() throws Exception {
        String subdomain = uniqueSubdomain("rules");
        TenantId id = api.registered(platformToken, subdomain);
        String owner = api.staffToken(id, subdomain, "OWNER");
        String takenByOther = uniqueSubdomain("other");
        api.registered(platformToken, takenByOther);

        expectProblem(api.changeSubdomain(owner, subdomain, "a--b"), 422, "SUBDOMAIN_INVALID");
        expectProblem(api.changeSubdomain(owner, subdomain, "www"), 422, "SUBDOMAIN_RESERVED");
        expectProblem(api.changeSubdomain(owner, subdomain, takenByOther), 409, "SUBDOMAIN_TAKEN");
        expectProblem(api.changeSubdomain(owner, subdomain, ""), 400, "INVALID_REQUEST");

        assertThat(subdomainsOf(id)).containsExactly(subdomain);
    }

    /** Criterion 21: two complexes changing to the same subdomain at once: one gets it, the other one is a 409. */
    @Test
    void concurrentChangesToTheSameSubdomainHaveOneWinner() throws Exception {
        String first = uniqueSubdomain("c1");
        String second = uniqueSubdomain("c2");
        String ownerOfFirst = api.staffToken(api.registered(platformToken, first), first, "OWNER");
        String ownerOfSecond = api.staffToken(api.registered(platformToken, second), second, "OWNER");
        String wanted = uniqueSubdomain("wanted");
        CyclicBarrier start = new CyclicBarrier(2);

        List<Integer> statuses = runTogether(
                changeWhenReady(start, ownerOfFirst, first, wanted),
                changeWhenReady(start, ownerOfSecond, second, wanted));

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(TenantDatabase.admin()
                .sql("SELECT count(*) FROM global.tenant_domain WHERE subdomain = :subdomain")
                .param("subdomain", wanted)
                .query(Long.class)
                .single()).isOne();
    }

    private Callable<Integer> changeWhenReady(CyclicBarrier start, String token, String on, String wanted) {
        return () -> {
            start.await();
            return api.changeSubdomain(token, on, wanted).andReturn().getResponse().getStatus();
        };
    }

    private static List<Integer> runTogether(Callable<Integer> first, Callable<Integer> second) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = List.of(executor.submit(first), executor.submit(second));
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            return statuses;
        }
    }

    private static List<String> subdomainsOf(TenantId tenant) {
        return TenantDatabase.admin().sql("SELECT subdomain FROM global.tenant_domain WHERE tenant_id = :id::uuid")
                .param("id", tenant.value())
                .query(String.class)
                .list();
    }

    private static void expectAvailability(ResultActions result, String subdomain, boolean available, String reason)
            throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.subdomain").value(subdomain))
                .andExpect(jsonPath("$.available").value(available))
                .andExpect(reason == null ? jsonPath("$.reason").isEmpty() : jsonPath("$.reason").value(reason));
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(code));
    }
}
