package com.kuador.platodo.api.subscriptions.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.FieldNameTakenException;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;

/** Stored plan field definitions. Labels are unique without regard to case, and keys are unique. */
public interface PlanFieldDefinitions {

    /** Every definition, retired ones included: basic fields in their fixed order, then custom fields by label. */
    List<PlanFieldDefinition> all();

    /** The definition, locked until the current transaction ends. */
    Optional<PlanFieldDefinition> lock(UUID id);

    /** @throws FieldNameTakenException if another field has the same label or key */
    void insert(PlanFieldDefinition field);

    /** @throws FieldNameTakenException if another field has the same label */
    void update(PlanFieldDefinition field);
}
