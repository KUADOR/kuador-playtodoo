package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.util.Objects;

/**
 * Base URL of the web of a complex, from {@code playtodoo.tenant-url-pattern} (for example
 * {@code https://{subdomain}.playtodoo.com}, or {@code http://{subdomain}.localhost:8000} on a workstation).
 */
final class TenantUrls {

    static final String SUBDOMAIN_PLACEHOLDER = "{subdomain}";

    private final String pattern;

    TenantUrls(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (!pattern.contains(SUBDOMAIN_PLACEHOLDER)) {
            throw new IllegalArgumentException("The tenant URL pattern must contain " + SUBDOMAIN_PLACEHOLDER);
        }
        this.pattern = pattern;
    }

    String of(String subdomain) {
        return pattern.replace(SUBDOMAIN_PLACEHOLDER, subdomain);
    }
}
