package com.kuador.platodo.api.subscriptions.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One plan or status change of a subscription, kept for one year (PLATODO-27 CA5, CA6).
 *
 * @param before plan name or status before the change; null for the first assignment, at registration
 * @param after  plan name or status after the change
 */
public record SubscriptionHistoryEntry(UUID id, UUID tenantId, Kind kind, String before, String after,
        UUID changedBy, Instant changedAt) {

    /** What changed. */
    public enum Kind {
        PLAN,
        STATUS
    }

    public SubscriptionHistoryEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(changedBy, "changedBy");
        Objects.requireNonNull(changedAt, "changedAt");
    }

    /** The plan and status a complex starts with when it is registered. */
    public static List<SubscriptionHistoryEntry> ofRegistration(Subscription started) {
        return List.of(
            new SubscriptionHistoryEntry(UUID.randomUUID(), started.tenantId(), Kind.PLAN, null,
                    started.plan().name(), started.statusChangedBy(), started.createdAt()),
            new SubscriptionHistoryEntry(UUID.randomUUID(), started.tenantId(), Kind.STATUS, null,
                    started.status().name(), started.statusChangedBy(), started.createdAt()));
    }

    /** The move from the plan of {@code before} to the plan of {@code after}. */
    public static SubscriptionHistoryEntry ofPlanChange(Subscription before, Subscription after, UUID changedBy) {
        return new SubscriptionHistoryEntry(UUID.randomUUID(), after.tenantId(), Kind.PLAN, before.plan().name(),
                after.plan().name(), changedBy, after.plan().assignedAt());
    }

    /** The move from the status of {@code before} to the status of {@code after}. */
    public static SubscriptionHistoryEntry ofStatusChange(Subscription before, Subscription after) {
        return new SubscriptionHistoryEntry(UUID.randomUUID(), after.tenantId(), Kind.STATUS, before.status().name(),
                after.status().name(), after.statusChangedBy(), after.statusSince());
    }
}
