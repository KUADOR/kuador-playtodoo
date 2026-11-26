package com.kuador.platodo.api.identity.application.port.in;

import com.kuador.platodo.api.identity.domain.Capability;

/** An operator lacks, or no longer has, the capability an operation needs ({@code 403 CAPABILITY_REVOKED}). */
public final class CapabilityRevokedException extends RuntimeException {

    private final Capability capability;

    public CapabilityRevokedException(Capability capability) {
        super("Operators do not have capability " + capability);
        this.capability = capability;
    }

    public Capability capability() {
        return capability;
    }
}
