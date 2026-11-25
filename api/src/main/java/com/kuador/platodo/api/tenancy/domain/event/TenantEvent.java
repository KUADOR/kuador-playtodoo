package com.kuador.platodo.api.tenancy.domain.event;

import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Lifecycle event published by Tenancy (catalog of events in docs/architecture.md). It carries the tenant id so that
 * a consumer that touches complex data opens its own {@code TenantScope}; the scope is not inherited across threads.
 * Events never carry personal data.
 */
public interface TenantEvent {

    TenantId tenantId();

    Instant occurredAt();
}
