package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.util.List;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Paths of the api and the host context each one belongs to ({@code contracts/tenancy.openapi.yaml}). Every
 * {@value #API} path is a tenant-context operation except the global ones listed here.
 */
final class TenancyApiPaths {

    static final String API = "/api";
    static final String V1 = API + "/v1";
    static final String TENANT_STATE = V1 + "/tenant";

    private static final List<PathPattern> GLOBAL_CONTEXT = parse(
            V1 + "/auth/players/**",
            V1 + "/auth/platform/**",
            V1 + "/subdomains/**",
            V1 + "/platform/**");

    private TenancyApiPaths() {
    }

    /** Whether {@code path} (within the application) is an operation of the global host. */
    static boolean isGlobalContext(String path) {
        PathContainer container = PathContainer.parsePath(path);
        return GLOBAL_CONTEXT.stream().anyMatch(pattern -> pattern.matches(container));
    }

    private static List<PathPattern> parse(String... patterns) {
        PathPatternParser parser = PathPatternParser.defaultInstance;
        return List.of(patterns).stream().map(parser::parse).toList();
    }
}
