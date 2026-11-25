package com.kuador.platodo.api.tenancy.domain.event;

import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Permanent deletion of a complex (ADR-013): its schema was dropped and only its tombstone is left. Unlike the other
 * lifecycle events, it is delivered synchronously inside the purge transaction, so that every module that keeps data
 * of the complex in the global schema removes its personal data before the complex is {@code PURGED}; a failing
 * consumer rolls the purge back and the job retries it.
 */
public record TenantPurged(TenantId tenantId, Instant purgedAt) implements TenantEvent {

    @Override
    public Instant occurredAt() {
        return purgedAt;
    }
}
