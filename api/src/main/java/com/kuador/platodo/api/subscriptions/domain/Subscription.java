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
     * The subscription moved by hand to {@code target} by {@code changedBy} at {@code now} (ADR-014). The plan stays.
     *
     * @param complexAcceptsChanges see {@link #allowedTransitions(boolean)}
     * @throws TransitionNotAllowedException if {@code target} is not among the allowed transitions
     */
    public Subscription changeStatus(SubscriptionStatus target, UUID changedBy, Instant now,
            boolean complexAcceptsChanges) {
        if (!allowedTransitions(complexAcceptsChanges).contains(target)) {
            throw new TransitionNotAllowedException(status, target);
        }
        return new Subscription(id, tenantId, plan, target, now, changedBy, createdAt, version + 1);
    }

    /**
     * The subscription with the conditions of another plan (ADR-018). The status stays.
     *
     * @throws SubscriptionCancelledException if the subscription is cancelled
     * @throws SamePlanException              if {@code newPlan} comes from the plan it already has
     */
    public Subscription changePlan(PlanSnapshot newPlan) {
        if (status == SubscriptionStatus.CANCELLED) {
            throw new SubscriptionCancelledException();
        }
        if (newPlan.planId().equals(plan.planId())) {
            throw new SamePlanException();
        }
        return new Subscription(id, tenantId, newPlan, status, statusSince, statusChangedBy, createdAt, version + 1);
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
