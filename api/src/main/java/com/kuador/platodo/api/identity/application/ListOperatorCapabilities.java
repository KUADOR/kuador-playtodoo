package com.kuador.platodo.api.identity.application;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilities;
import com.kuador.platodo.api.identity.domain.OperatorCapabilitySettings;

/** {@code GET /operator-capabilities}: SUPER_ADMIN only. */
public final class ListOperatorCapabilities {

    private final CapabilityGuard guard;
    private final OperatorCapabilities operatorCapabilities;

    public ListOperatorCapabilities(CapabilityGuard guard, OperatorCapabilities operatorCapabilities) {
        this.guard = guard;
        this.operatorCapabilities = operatorCapabilities;
    }

    public OperatorCapabilitySettings list() {
        guard.requireSuperAdmin();
        return operatorCapabilities.current();
    }
}
