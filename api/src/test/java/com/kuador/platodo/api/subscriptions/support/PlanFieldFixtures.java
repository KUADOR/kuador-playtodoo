package com.kuador.platodo.api.subscriptions.support;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.PlanFieldDefinition;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldOrigin;
import com.kuador.platodo.api.subscriptions.domain.PlanFieldType;

/** Plan field definitions as the migration seeds them, plus custom ones for tests. */
public final class PlanFieldFixtures {

    private PlanFieldFixtures() {
    }

    public static List<PlanFieldDefinition> basicFields() {
        return List.of(basic("name", PlanFieldType.TEXT), basic("price", PlanFieldType.NUMBER),
                basic("currency", PlanFieldType.TEXT), basic("periodicity", PlanFieldType.TEXT),
                basic("courtLimit", PlanFieldType.NUMBER), basic("userLimit", PlanFieldType.NUMBER));
    }

    public static PlanFieldDefinition basic(String key, PlanFieldType type) {
        return new PlanFieldDefinition(UUID.randomUUID(), key, key, type, true, PlanFieldOrigin.BASIC,
                Optional.empty(), 0);
    }

    public static PlanFieldDefinition custom(String label, PlanFieldType type, boolean required) {
        return PlanFieldDefinition.custom(UUID.randomUUID(), label, type, required);
    }

    public static PlanFieldDefinition retired(String label, PlanFieldType type) {
        return custom(label, type, true).retire(Instant.parse("2027-01-15T10:00:00Z"));
    }
}
