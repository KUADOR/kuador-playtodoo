package com.kuador.platodo.api.subscriptions.application;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanOptionsSource;
import com.kuador.platodo.api.subscriptions.domain.PlanOptions;

/** {@code GET /plan-options}: any platform user. */
public final class GetPlanOptions {

    private final CapabilityGuard guard;
    private final PlanOptionsSource options;

    public GetPlanOptions(CapabilityGuard guard, PlanOptionsSource options) {
        this.guard = guard;
        this.options = options;
    }

    public PlanOptions get() {
        guard.requirePlatformUser();
        return options.current();
    }
}
