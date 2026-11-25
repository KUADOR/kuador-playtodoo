package com.kuador.platodo.api.tenancy.application;

/** The subdomain is used, or was used, by a complex: subdomains are never reused (ADR-012). */
public class SubdomainTakenException extends RuntimeException {

    public SubdomainTakenException(Throwable cause) {
        super("The subdomain is used or was used by a complex", cause, false, false);
    }
}
