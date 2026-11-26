package com.kuador.platodo.api.subscriptions.application;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.domain.BasicFieldReadOnlyException;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;

/**
 * {@code POST /plan-fields/{fieldId}/retirement}: SUPER_ADMIN only. A retired field is no longer offered nor required;
 * the values plans already have are kept. Retiring a retired field changes nothing.
 */
public final class RetirePlanField {

    private final CapabilityGuard guard;
    private final PlanFieldDefinitions fields;
    private final TransactionOperations globalTransaction;
    private final Clock clock;

    public RetirePlanField(CapabilityGuard guard, PlanFieldDefinitions fields,
            TransactionOperations globalTransaction, Clock clock) {
        this.guard = guard;
        this.fields = fields;
        this.globalTransaction = globalTransaction;
        this.clock = clock;
    }

    /**
     * @throws PlanFieldNotFoundException  if there is no such field
     * @throws BasicFieldReadOnlyException if it is a basic field
     */
    public PlanFieldDefinition retire(UUID fieldId) {
        guard.requireSuperAdmin();
        return globalTransaction.execute(status -> {
            PlanFieldDefinition field = fields.lock(fieldId).orElseThrow(PlanFieldNotFoundException::new);
            PlanFieldDefinition retired = field.retire(clock.instant());
            if (retired != field) {
                fields.update(retired);
            }
            return retired;
        });
    }
}
