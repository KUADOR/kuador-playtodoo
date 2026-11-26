package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;

/** {@code GET /complexes/{complexId}}: every platform user. */
public final class GetComplex {

    private final CapabilityGuard guard;
    private final ComplexDetails details;

    public GetComplex(CapabilityGuard guard, ComplexDetails details) {
        this.guard = guard;
        this.details = details;
    }

    /** @throws ComplexNotFoundException if no complex has this id */
    public ComplexDetail get(UUID complexId) {
        guard.requirePlatformUser();
        return details.of(complexId);
    }
}
