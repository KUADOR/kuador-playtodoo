package com.kuador.platodo.api.subscriptions.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Valid fields of a plan, as {@link PlanContentRules} accepts them. Only {@code name} is always present; the other
 * basic fields are {@code null} while unset. {@code customFields} holds values by field key, ordered by key, including
 * the values kept for retired fields.
 */
public record PlanContent(String name, BigDecimal price, String currency, String periodicity, Integer courtLimit,
        Integer userLimit, Map<String, Object> customFields) {

    public PlanContent {
        Objects.requireNonNull(name, "name");
        customFields = Collections.unmodifiableMap(new TreeMap<>(customFields));
    }
}
