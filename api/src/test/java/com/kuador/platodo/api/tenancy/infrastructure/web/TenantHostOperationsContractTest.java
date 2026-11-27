package com.kuador.platodo.api.tenancy.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * The operationIds the audit records for support requests (ADR-020) are the ones of the contracts: every operation
 * served on the host of a complex, and only those.
 */
class TenantHostOperationsContractTest {

    private static final Path CONTRACTS = Path.of("..", "contracts");
    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");
    private static final String TENANT_HOST_SERVER = "{subdomain}.playtodoo.com";

    @Test
    void theTableHoldsExactlyTheTenantHostOperationsOfTheContracts() throws IOException {
        Map<String, String> fromContracts = new HashMap<>();
        fromContracts.putAll(operationsOf("bookings.openapi.yaml", true));
        fromContracts.putAll(operationsOf("tenancy.openapi.yaml", false));

        assertThat(TenantHostOperations.all()).isEqualTo(fromContracts);
    }

    @Test
    void aRouteThatIsNotAContractOperationHasNoOperationId() {
        assertThat(TenantHostOperations.operationIdOf("GET", "/api/v1/staff/bookings")).contains("listStaffBookings");
        assertThat(TenantHostOperations.operationIdOf("DELETE", "/api/v1/staff/bookings")).isEmpty();
        assertThat(TenantHostOperations.operationIdOf("GET", SupportAccessGuard.UNMATCHED_ROUTE)).isEmpty();
    }

    /**
     * The operations of {@code contract} keyed like {@link TenantHostOperations}: all of them when the whole contract
     * is served on tenant hosts, otherwise only the paths whose own server is the tenant host.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> operationsOf(String contract, boolean wholeContractOnTenantHost)
            throws IOException {
        Map<String, Object> document;
        try (Reader reader = Files.newBufferedReader(CONTRACTS.resolve(contract))) {
            document = new Yaml().load(reader);
        }
        Map<String, String> operations = new HashMap<>();
        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) document.get("paths");
        paths.forEach((path, item) -> {
            if (wholeContractOnTenantHost || servedOnTenantHost(item)) {
                item.forEach((method, operation) -> {
                    if (HTTP_METHODS.contains(method)) {
                        operations.put(method.toUpperCase() + " " + TenancyApiPaths.V1 + path,
                                (String) ((Map<String, Object>) operation).get("operationId"));
                    }
                });
            }
        });
        return operations;
    }

    @SuppressWarnings("unchecked")
    private static boolean servedOnTenantHost(Map<String, Object> pathItem) {
        List<Map<String, Object>> servers = (List<Map<String, Object>>) pathItem.getOrDefault("servers", List.of());
        return servers.stream().anyMatch(server -> String.valueOf(server.get("url")).contains(TENANT_HOST_SERVER));
    }
}
