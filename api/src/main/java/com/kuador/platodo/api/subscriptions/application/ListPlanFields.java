package com.kuador.platodo.api.subscriptions.application;

import java.util.List;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;

/** {@code GET /plan-fields}: SUPER_ADMIN only. Basic fields first, then custom fields by label, retired included. */
public final class ListPlanFields {

    private final CapabilityGuard guard;
    private final PlanFieldDefinitions fields;

    public ListPlanFields(CapabilityGuard guard, PlanFieldDefinitions fields) {
        this.guard = guard;
        this.fields = fields;
    }

    public List<PlanFieldDefinition> list() {
        guard.requireSuperAdmin();
        return fields.all();
    }
}
