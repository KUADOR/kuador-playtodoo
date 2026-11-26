package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.domain.BasicFieldReadOnlyException;
import com.kuador.platodo.api.subscriptions.domain.InvalidPlanFieldsException;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;

/**
 * {@code PUT /plan-fields/{fieldId}}: SUPER_ADMIN only. Changes the label or the required flag of a custom field with
 * optimistic concurrency. Making a field required does not unpublish plans: it is checked the next time a plan is
 * published or edited.
 */
public final class UpdatePlanField {

    private final CapabilityGuard guard;
    private final PlanFieldDefinitions fields;
    private final TransactionOperations globalTransaction;

    public UpdatePlanField(CapabilityGuard guard, PlanFieldDefinitions fields,
            TransactionOperations globalTransaction) {
        this.guard = guard;
        this.fields = fields;
        this.globalTransaction = globalTransaction;
    }

    /**
     * @throws PlanFieldNotFoundException  if there is no such field
     * @throws BasicFieldReadOnlyException if it is a basic field
     * @throws InvalidPlanFieldsException  if the label is blank or too long
     * @throws StaleVersionException       if {@code expectedVersion} is not the current version
     * @throws FieldNameTakenException     if another field has this label
     */
    public PlanFieldDefinition update(UUID fieldId, long expectedVersion, String label, boolean required) {
        guard.requireSuperAdmin();
        return globalTransaction.execute(status -> {
            PlanFieldDefinition field = fields.lock(fieldId).orElseThrow(PlanFieldNotFoundException::new);
            PlanFieldDefinition relabelled = field.relabel(label, required);
            if (field.version() != expectedVersion) {
                throw new StaleVersionException();
            }
            if (relabelled != field) {
                fields.update(relabelled);
            }
            return relabelled;
        });
    }
}
