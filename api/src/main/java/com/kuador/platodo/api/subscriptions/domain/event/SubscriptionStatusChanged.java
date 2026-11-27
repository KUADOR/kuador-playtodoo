package com.kuador.platodo.api.subscriptions.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.SubscriptionStatus;

/**
 * A Kuador team member changed the status of a subscription by hand (ADR-014). Published inside the transaction that
 * records the change, so consumers listening synchronously (Tenancy applies the effect on the tenant) commit or roll
 * back with it.
 *
 * @param tenantId the complex, by its tenant id
 */
public record SubscriptionStatusChanged(UUID tenantId, SubscriptionStatus from, SubscriptionStatus to,
        UUID changedBy, Instant at) implements SubscriptionEvent {

    public SubscriptionStatusChanged {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(changedBy, "changedBy");
        Objects.requireNonNull(at, "at");
    }
}
