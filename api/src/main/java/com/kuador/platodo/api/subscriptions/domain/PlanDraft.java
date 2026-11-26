package com.kuador.platodo.api.subscriptions.domain;

import java.util.Map;

/**
 * Fields of a plan as the caller sent them, before {@link PlanContentRules} checks them. Any of them may be
 * {@code null}. {@code price} is the decimal text of the contract. Each custom value should be a {@link String}, a
 * {@link java.math.BigDecimal} or a {@link Boolean}; anything else is reported as the wrong type.
 */
public record PlanDraft(String name, String price, String currency, String periodicity, Integer courtLimit,
        Integer userLimit, Map<String, Object> customFields) {

    public PlanDraft {
        customFields = customFields == null ? Map.of() : customFields;
    }
}
