package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;

import com.kuador.platodo.api.subscriptions.application.FieldNameTakenException;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldOrigin;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/**
 * Plan field definitions through the global persistence unit. Uniqueness of labels (without regard to case) and keys
 * is enforced by unique indexes, so concurrent requests never create two fields with the same name.
 */
public final class JpaPlanFieldDefinitions implements PlanFieldDefinitions {

    private static final Set<String> NAME_UNIQUE_INDEXES =
            Set.of("plan_field_definition_key_unique", "plan_field_definition_label_unique");
    private static final String ALL = """
            SELECT f FROM PlanFieldDefinitionEntity f
            ORDER BY CASE WHEN f.basicOrder IS NULL THEN 1 ELSE 0 END, f.basicOrder, lower(f.label), f.id
            """;

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaPlanFieldDefinitions(EntityManager global) {
        this.global = global;
    }

    @Override
    public List<PlanFieldDefinition> all() {
        return global.createQuery(ALL, PlanFieldDefinitionEntity.class).getResultList().stream()
                .map(JpaPlanFieldDefinitions::toDefinition)
                .toList();
    }

    @Override
    public Optional<PlanFieldDefinition> lock(UUID id) {
        return Optional.ofNullable(global.find(PlanFieldDefinitionEntity.class, id, LockModeType.PESSIMISTIC_WRITE))
                .map(JpaPlanFieldDefinitions::toDefinition);
    }

    @Override
    public void insert(PlanFieldDefinition field) {
        PlanFieldDefinitionEntity row =
                PlanFieldDefinitionEntity.custom(field.id(), field.key(), field.type().name());
        apply(row, field);
        global.persist(row);
        flushTranslatingNameTaken();
    }

    @Override
    public void update(PlanFieldDefinition field) {
        PlanFieldDefinitionEntity row = global.find(PlanFieldDefinitionEntity.class, field.id());
        if (row == null) {
            throw new IllegalStateException("Plan field " + field.id() + " does not exist");
        }
        apply(row, field);
        flushTranslatingNameTaken();
    }

    private static void apply(PlanFieldDefinitionEntity row, PlanFieldDefinition field) {
        row.change(field.label(), field.required(), field.retiredAt().orElse(null), field.version());
    }

    private void flushTranslatingNameTaken() {
        try {
            global.flush();
        } catch (ConstraintViolationException violation) {
            if (NAME_UNIQUE_INDEXES.contains(violation.getConstraintName())) {
                throw new FieldNameTakenException();
            }
            throw violation;
        }
    }

    private static PlanFieldDefinition toDefinition(PlanFieldDefinitionEntity row) {
        return new PlanFieldDefinition(row.id(), row.key(), row.label(), PlanFieldType.valueOf(row.type()),
                row.required(), PlanFieldOrigin.valueOf(row.origin()), Optional.ofNullable(row.retiredAt()),
                row.version());
    }
}
