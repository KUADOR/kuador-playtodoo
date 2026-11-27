package com.kuador.platodo.api.subscriptions.domain;

/**
 * The target status is not among the allowed transitions of the subscription ({@code 422 TRANSITION_NOT_ALLOWED}):
 * it is outside the table of ADR-014, the subscription is cancelled, or the complex does not accept changes now.
 */
public final class TransitionNotAllowedException extends RuntimeException {

    public TransitionNotAllowedException(SubscriptionStatus from, SubscriptionStatus to) {
        super("Transition not allowed: " + from + " to " + to, null, false, false);
    }
}
