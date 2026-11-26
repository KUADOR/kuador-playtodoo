package com.kuador.platodo.api.subscriptions.application;

import java.util.Optional;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PlanPage;
import com.kuador.platodo.api.subscriptions.domain.PlanStatus;

/**
 * {@code GET /plans}: the SUPER_ADMIN sees every plan; any other platform user, who needs them to register a
 * complex, may only list the published ones.
 */
public final class ListPlans {

    private final CapabilityGuard guard;
    private final Plans plans;

    public ListPlans(CapabilityGuard guard, Plans plans) {
        this.guard = guard;
        this.plans = plans;
    }

    /** @throws PlatformCallerForbiddenException if an operator asks for something other than published plans */
    public PlanPage list(Optional<PlanStatus> status, PageRequest page) {
        PlatformCaller caller = guard.requirePlatformUser();
        if (!caller.isSuperAdmin() && status.filter(PlanStatus.PUBLISHED::equals).isEmpty()) {
            throw new PlatformCallerForbiddenException();
        }
        return plans.page(status, page);
    }
}
