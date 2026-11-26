package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanOptionsSource;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.domain.InvalidPlanFieldsException;
import com.kuador.platodo.api.subscriptions.domain.Plan;
import com.kuador.platodo.api.subscriptions.domain.PlanContentRules;
import com.kuador.platodo.api.subscriptions.domain.PlanDraft;

/** {@code POST /plans}: SUPER_ADMIN only. Creates a draft, which needs only a unique name (ADR-018). */
public final class CreatePlan {

    private final CapabilityGuard guard;
    private final Plans plans;
    private final PlanFieldDefinitions fields;
    private final PlanOptionsSource options;
    private final TransactionOperations globalTransaction;

    public CreatePlan(CapabilityGuard guard, Plans plans, PlanFieldDefinitions fields, PlanOptionsSource options,
            TransactionOperations globalTransaction) {
        this.guard = guard;
        this.plans = plans;
        this.fields = fields;
        this.options = options;
        this.globalTransaction = globalTransaction;
    }

    /**
     * @throws InvalidPlanFieldsException if a field is invalid
     * @throws PlanNameTakenException     if another plan has this name
     */
    public Plan create(PlanDraft draft) {
        guard.requireSuperAdmin();
        return globalTransaction.execute(status -> {
            PlanContentRules rules = new PlanContentRules(options.current(), fields.all());
            Plan plan = Plan.draft(UUID.randomUUID(), rules.check(draft));
            plans.insert(plan);
            return plan;
        });
    }
}
