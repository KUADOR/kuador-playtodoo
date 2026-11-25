package com.kuador.platodo.api.tenancy.domain.event;

import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Kuador suspended an active complex. */
public record TenantSuspended(TenantId tenantId, Instant occurredAt) implements TenantEvent {
}
