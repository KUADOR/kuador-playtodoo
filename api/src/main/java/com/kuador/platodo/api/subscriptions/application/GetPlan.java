package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.domain.Plan;

/** {@code GET /plans/{planId}}: the SUPER_ADMIN sees any plan; other platform users only published ones. */
public final class GetPlan {

    private final CapabilityGuard guard;
    private final Plans plans;

    public GetPlan(CapabilityGuard guard, Plans plans) {
        this.guard = guard;
        this.plans = plans;
    }

    /** @throws PlanNotFoundException if there is no such plan, or it is a draft and the caller is an operator */
    public Plan get(UUID planId) {
        PlatformCaller caller = guard.requirePlatformUser();
        return plans.find(planId)
                .filter(plan -> caller.isSuperAdmin() || plan.isPublished())
                .orElseThrow(PlanNotFoundException::new);
    }
}
