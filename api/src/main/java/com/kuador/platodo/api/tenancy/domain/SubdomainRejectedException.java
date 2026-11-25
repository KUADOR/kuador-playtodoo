package com.kuador.platodo.api.tenancy.domain;

import java.util.Objects;

/** A candidate breaks the subdomain rules (ADR-012); {@link #rejection()} says which one. */
public class SubdomainRejectedException extends RuntimeException {

    private final SubdomainRules.Rejection rejection;

    public SubdomainRejectedException(SubdomainRules.Rejection rejection) {
        super("Subdomain rejected: " + rejection, null, false, false);
        this.rejection = Objects.requireNonNull(rejection, "rejection");
    }

    public SubdomainRules.Rejection rejection() {
        return rejection;
    }
}
