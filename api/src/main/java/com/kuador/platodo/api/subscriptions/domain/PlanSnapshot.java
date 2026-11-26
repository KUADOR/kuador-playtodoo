package com.kuador.platodo.api.subscriptions.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The conditions of a published plan fixed when it is assigned to a complex (ADR-018): later edits of the plan only
 * affect new assignments. {@code customFields} holds every value the plan had, by field key.
 */
public record PlanSnapshot(UUID planId, String name, BigDecimal price, String currency, String periodicity,
        int courtLimit, int userLimit, Map<String, Object> customFields, Instant assignedAt) {

    public PlanSnapshot {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(periodicity, "periodicity");
        Objects.requireNonNull(assignedAt, "assignedAt");
        customFields = Collections.unmodifiableMap(new TreeMap<>(customFields));
    }

    /**
     * The conditions of {@code plan} assigned {@code at}.
     *
     * @throws PlanNotPublishedException if the plan is a draft
     */
    public static PlanSnapshot of(Plan plan, Instant at) {
        if (!plan.isPublished()) {
            throw new PlanNotPublishedException();
        }
        PlanContent content = plan.content();
        return new PlanSnapshot(plan.id(), content.name(), content.price(), content.currency(), content.periodicity(),
                content.courtLimit(), content.userLimit(), content.customFields(), at);
    }
}
