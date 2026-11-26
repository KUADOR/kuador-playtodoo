package com.kuador.platodo.api.subscriptions.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Commercial status of a subscription (ADR-014). It is changed only by hand; {@link #manualTransitions()} lists the
 * statuses each one can move to. {@code CANCELLED} is terminal: a cancelled subscription is never reactivated.
 */
public enum SubscriptionStatus {
    ACTIVE,
    EXPIRING,
    SUSPENDED,
    CANCELLED;

    /** The statuses this one can move to by hand, before looking at the lifecycle of the complex. */
    public Set<SubscriptionStatus> manualTransitions() {
        return switch (this) {
            case ACTIVE -> EnumSet.of(EXPIRING, SUSPENDED, CANCELLED);
            case EXPIRING -> EnumSet.of(ACTIVE, SUSPENDED, CANCELLED);
            case SUSPENDED -> EnumSet.of(ACTIVE, CANCELLED);
            case CANCELLED -> EnumSet.noneOf(SubscriptionStatus.class);
        };
    }
}
