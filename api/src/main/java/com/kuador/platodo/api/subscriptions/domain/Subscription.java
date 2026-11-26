package com.kuador.platodo.api.subscriptions.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The subscription of a complex (ADR-014): the plan conditions it has and its commercial status, with when and by
 * whom that status was set. A complex has exactly one, created {@code ACTIVE} with its registration.
 *
 * @param tenantId        the complex, by its tenant id
 * @param statusChangedBy the Kuador team member who set the current status
 * @param version         raised by every change, for optimistic concurrency
 */
public record Subscription(UUID id, UUID tenantId, PlanSnapshot plan, SubscriptionStatus status, Instant statusSince,
        UUID statusChangedBy, Instant createdAt, long version) {

    public Subscription {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(statusSince, "statusSince");
        Objects.requireNonNull(statusChangedBy, "statusChangedBy");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** The subscription a complex starts with when {@code registeredBy} registers it with {@code plan}. */
    public static Subscription start(UUID id, UUID tenantId, PlanSnapshot plan, UUID registeredBy, Instant now) {
        return new Subscription(id, tenantId, plan, SubscriptionStatus.ACTIVE, now, registeredBy, now, 0);
    }

    /**
     * The statuses the subscription can move to now.
     *
     * @param complexAcceptsChanges whether the lifecycle of the complex allows commercial changes: it is provisioned,
     *                              not being purged, and its retention, if any, has not expired
     */
    public Set<SubscriptionStatus> allowedTransitions(boolean complexAcceptsChanges) {
        return complexAcceptsChanges ? status.manualTransitions() : EnumSet.noneOf(SubscriptionStatus.class);
    }
}
