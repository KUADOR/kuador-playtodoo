package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.domain.Plan;
import com.kuador.platodo.api.subscriptions.domain.PlanIncompleteException;

/**
 * {@code POST /plans/{planId}/publication}: SUPER_ADMIN only. Publishes a complete draft so it can be assigned;
 * publishing an already published plan changes nothing.
 */
public final class PublishPlan {

    private final CapabilityGuard guard;
    private final Plans plans;
    private final PlanFieldDefinitions fields;
    private final TransactionOperations globalTransaction;

    public PublishPlan(CapabilityGuard guard, Plans plans, PlanFieldDefinitions fields,
            TransactionOperations globalTransaction) {
        this.guard = guard;
        this.plans = plans;
        this.fields = fields;
        this.globalTransaction = globalTransaction;
    }

    /**
     * @throws PlanNotFoundException   if there is no such plan
     * @throws PlanIncompleteException if a basic field or a required custom field is empty
     */
    public Plan publish(UUID planId) {
        guard.requireSuperAdmin();
        return globalTransaction.execute(status -> {
            Plan plan = plans.lock(planId).orElseThrow(PlanNotFoundException::new);
            Plan published = plan.publish(fields.all());
            if (published != plan) {
                plans.update(published);
            }
            return published;
        });
    }
}
