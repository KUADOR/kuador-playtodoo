package com.kuador.platodo.api.subscriptions.infrastructure.web;

import com.kuador.platodo.api.subscriptions.domain.SubscriptionStatus;

/** Schema {@code ChangeStatusRequest}; both properties are required. An unknown status is a malformed body. */
record ChangeStatusRequest(SubscriptionStatus targetStatus, Long version) {

    /** @throws InvalidSubscriptionsRequestException if a property is missing */
    ChangeStatusRequest requireComplete() {
        if (targetStatus == null || version == null || version < 0) {
            throw new InvalidSubscriptionsRequestException("`targetStatus` and `version` (>= 0) are required.");
        }
        return this;
    }
}
