package com.kuador.platodo.api.tenancy.domain.event;

import java.time.Instant;
import java.util.Objects;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * The lifecycle status of a complex changed (ADR-014). Unlike the other lifecycle events, it is delivered
 * synchronously inside the transaction of the change, so that what depends on the status (the visibility of the
 * complex in the catalog, the support sessions) never drifts from it: a failing consumer rolls the change back.
 *
 * @param purgeDueAt instant from which the purge may delete the schema; null when the new status has no retention
 */
public record TenantStatusChanged(TenantId tenantId, TenantStatus status, Instant purgeDueAt, Instant occurredAt)
        implements TenantEvent {

    public TenantStatusChanged {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
