package com.kuador.platodo.api.subscriptions.application;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanOptionsSource;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.domain.InvalidPlanFieldsException;
import com.kuador.platodo.api.subscriptions.domain.Plan;
import com.kuador.platodo.api.subscriptions.domain.PlanContent;
import com.kuador.platodo.api.subscriptions.domain.PlanContentRules;
import com.kuador.platodo.api.subscriptions.domain.PlanDraft;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;
import com.kuador.platodo.api.subscriptions.domain.PlanIncompleteException;

/**
 * {@code PUT /plans/{planId}}: SUPER_ADMIN only. Replaces the fields of a plan with optimistic concurrency. A
 * published plan stays published and complete; the subscriptions that already have it keep their snapshot (ADR-018).
 */
public final class UpdatePlan {

    private final CapabilityGuard guard;
    private final Plans plans;
    private final PlanFieldDefinitions fields;
    private final PlanOptionsSource options;
    private final TransactionOperations globalTransaction;

    public UpdatePlan(CapabilityGuard guard, Plans plans, PlanFieldDefinitions fields, PlanOptionsSource options,
            TransactionOperations globalTransaction) {
        this.guard = guard;
        this.plans = plans;
        this.fields = fields;
        this.options = options;
        this.globalTransaction = globalTransaction;
    }

    /**
     * @throws PlanNotFoundException       if there is no such plan
     * @throws StaleVersionException       if {@code expectedVersion} is not the current version
     * @throws InvalidPlanFieldsException  if a field is invalid
     * @throws PlanIncompleteException     if the plan is published and a required field is left empty
     * @throws PlanNameTakenException      if another plan has this name
     */
    public Plan update(UUID planId, long expectedVersion, PlanDraft draft) {
        guard.requireSuperAdmin();
        return globalTransaction.execute(status -> {
            Plan plan = plans.lock(planId).orElseThrow(PlanNotFoundException::new);
            if (plan.version() != expectedVersion) {
                throw new StaleVersionException();
            }
            List<PlanFieldDefinition> definitions = fields.all();
            PlanContent content = new PlanContentRules(options.current(), definitions)
                    .check(draft, plan.content().customFields());
            Plan revised = plan.revise(content, definitions);
            plans.update(revised);
            return revised;
        });
    }
}
