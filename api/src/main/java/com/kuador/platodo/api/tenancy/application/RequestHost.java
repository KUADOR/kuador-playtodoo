package com.kuador.platodo.api.tenancy.application;

/** What a request host is for the api (ADR-002, ADR-012). */
public sealed interface RequestHost {

    /** {@code api.<root domain>}: global context, no tenant. */
    record Global() implements RequestHost {
    }

    /** {@code <label>.<root domain>}: the complex with that subdomain label, still to be validated and resolved. */
    record Tenant(String subdomainLabel) implements RequestHost {
    }

    /** Any other host (the root domain itself, nested labels, other domains): served by nothing in the api. */
    record Foreign() implements RequestHost {
    }
}
