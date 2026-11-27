package com.kuador.platodo.api.subscriptions.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The SUPER_ADMIN assigned another plan to a complex (ADR-018). Published inside the transaction that records the
 * change; consumers listen after the commit.
 *
 * @param tenantId the complex, by its tenant id
 */
public record SubscriptionPlanChanged(UUID tenantId, UUID fromPlanId, UUID toPlanId, UUID changedBy, Instant at)
        implements SubscriptionEvent {

    public SubscriptionPlanChanged {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(fromPlanId, "fromPlanId");
        Objects.requireNonNull(toPlanId, "toPlanId");
        Objects.requireNonNull(changedBy, "changedBy");
        Objects.requireNonNull(at, "at");
    }
}
