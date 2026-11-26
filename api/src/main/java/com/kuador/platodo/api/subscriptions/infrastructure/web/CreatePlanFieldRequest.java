package com.kuador.platodo.api.subscriptions.infrastructure.web;

import com.kuador.platodo.api.subscriptions.domain.PlanFieldType;

/**
 * Schema {@code CreatePlanFieldRequest}. A missing {@code type} or {@code required} fails with
 * {@link InvalidSubscriptionsRequestException}; an unknown type fails while reading the body. The label is checked
 * by the domain.
 */
record CreatePlanFieldRequest(String label, PlanFieldType type, Boolean required) {

    PlanFieldType requiredType() {
        if (type == null) {
            throw new InvalidSubscriptionsRequestException("`type` is required.");
        }
        return type;
    }

    boolean requiredFlag() {
        if (required == null) {
            throw new InvalidSubscriptionsRequestException("`required` is required.");
        }
        return required;
    }
}
