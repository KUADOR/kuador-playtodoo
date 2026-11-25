package com.kuador.platodo.api.tenancy.application;

import java.util.Locale;
import java.util.Objects;

/**
 * Classifies request hosts against the root domain of the environment ({@code playtodoo.root-domain}, for example
 * {@code playtodoo.com}, or {@code localhost} on a workstation). The host must already be the trusted one: the
 * web server only takes it from {@code X-Forwarded-Host} when the request comes from a configured proxy.
 */
public final class RequestHosts {

    /** Label of the global api host, {@code api.<root domain>}. */
    public static final String GLOBAL_LABEL = "api";

    private static final String LABEL_SEPARATOR = ".";
    private static final String FULLY_QUALIFIED_SUFFIX = ".";

    private final String rootSuffix;

    public RequestHosts(String rootDomain) {
        Objects.requireNonNull(rootDomain, "rootDomain");
        if (rootDomain.isBlank()) {
            throw new IllegalArgumentException("The root domain cannot be blank");
        }
        this.rootSuffix = LABEL_SEPARATOR + normalize(rootDomain);
    }

    public RequestHost classify(String host) {
        if (host == null) {
            return new RequestHost.Foreign();
        }
        String normalized = normalize(host);
        if (!normalized.endsWith(rootSuffix)) {
            return new RequestHost.Foreign();
        }
        String label = normalized.substring(0, normalized.length() - rootSuffix.length());
        if (label.isEmpty() || label.contains(LABEL_SEPARATOR)) {
            return new RequestHost.Foreign();
        }
        return GLOBAL_LABEL.equals(label) ? new RequestHost.Global() : new RequestHost.Tenant(label);
    }

    private static String normalize(String host) {
        String lowerCase = host.trim().toLowerCase(Locale.ROOT);
        return lowerCase.endsWith(FULLY_QUALIFIED_SUFFIX)
                ? lowerCase.substring(0, lowerCase.length() - FULLY_QUALIFIED_SUFFIX.length())
                : lowerCase;
    }
}
