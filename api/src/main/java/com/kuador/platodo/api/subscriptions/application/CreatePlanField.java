package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.domain.InvalidPlanFieldsException;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldType;

/** {@code POST /plan-fields}: SUPER_ADMIN only. Creates a custom field whose key is derived from its label. */
public final class CreatePlanField {

    private final CapabilityGuard guard;
    private final PlanFieldDefinitions fields;
    private final TransactionOperations globalTransaction;

    public CreatePlanField(CapabilityGuard guard, PlanFieldDefinitions fields,
            TransactionOperations globalTransaction) {
        this.guard = guard;
        this.fields = fields;
        this.globalTransaction = globalTransaction;
    }

    /**
     * @throws InvalidPlanFieldsException if the label is blank, too long or has no letter or digit
     * @throws FieldNameTakenException    if another field has this label or the key derived from it
     */
    public PlanFieldDefinition create(String label, PlanFieldType type, boolean required) {
        guard.requireSuperAdmin();
        PlanFieldDefinition field = PlanFieldDefinition.custom(UUID.randomUUID(), label, type, required);
        globalTransaction.executeWithoutResult(status -> fields.insert(field));
        return field;
    }
}
