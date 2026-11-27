package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.UUID;

/** Schema {@code ChangePlanRequest}; both properties are required. */
record ChangePlanRequest(UUID planId, Long version) {

    /** @throws InvalidSubscriptionsRequestException if a property is missing */
    ChangePlanRequest requireComplete() {
        if (planId == null || version == null || version < 0) {
            throw new InvalidSubscriptionsRequestException("`planId` and `version` (>= 0) are required.");
        }
        return this;
    }
}
