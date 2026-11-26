package com.kuador.platodo.api.tenancy.application.port.in;

import java.util.Objects;

/** A registration asked for a subdomain no new complex can take (ADR-012); {@link #reason()} says why. */
public class SubdomainUnavailableException extends RuntimeException {

    /** Why the subdomain cannot be taken. */
    public enum Reason {
        /** It breaks the format rules. */
        INVALID,
        /** It is in the reserved list. */
        RESERVED,
        /** A complex uses it, or used it: subdomains are never reused. */
        TAKEN
    }

    private final Reason reason;

    public SubdomainUnavailableException(Reason reason) {
        super("Subdomain unavailable: " + reason, null, false, false);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason reason() {
        return reason;
    }
}
