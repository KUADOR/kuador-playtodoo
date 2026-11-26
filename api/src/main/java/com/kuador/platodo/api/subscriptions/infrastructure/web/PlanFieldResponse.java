package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;

/** Schema {@code PlanField}. */
record PlanFieldResponse(UUID id, String key, String label, String type, boolean required, String origin,
        Instant retiredAt, long version) {

    static PlanFieldResponse of(PlanFieldDefinition field) {
        return new PlanFieldResponse(field.id(), field.key(), field.label(), field.type().name(), field.required(),
                field.origin().name(), field.retiredAt().orElse(null), field.version());
    }
}
