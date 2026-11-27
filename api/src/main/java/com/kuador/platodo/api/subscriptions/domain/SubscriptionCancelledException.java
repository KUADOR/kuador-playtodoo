package com.kuador.platodo.api.subscriptions.domain;

/** A cancelled subscription keeps its plan ({@code 422 SUBSCRIPTION_CANCELLED}). */
public final class SubscriptionCancelledException extends RuntimeException {

    public SubscriptionCancelledException() {
        super("The subscription is cancelled", null, false, false);
    }
}
