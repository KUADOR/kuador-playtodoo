package com.kuador.platodo.api.subscriptions.domain;

import java.util.List;

/**
 * A published plan, or one being published, lacks basic or required custom fields ({@code 422 PLAN_INCOMPLETE}).
 * Every field error is {@link FieldError.Code#REQUIRED}.
 */
public final class PlanIncompleteException extends RuntimeException {

    private final transient List<FieldError> missingFields;

    public PlanIncompleteException(List<FieldError> missingFields) {
        super("Missing fields: " + missingFields);
        this.missingFields = List.copyOf(missingFields);
    }

    public List<FieldError> missingFields() {
        return missingFields;
    }
}
