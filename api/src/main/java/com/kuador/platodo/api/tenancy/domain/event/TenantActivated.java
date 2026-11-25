package com.kuador.platodo.api.tenancy.domain.event;

import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** The complex became {@code ACTIVE}: provisioned, activated after a suspension or reactivated from the archive. */
public record TenantActivated(TenantId tenantId, String subdomain, Instant occurredAt) implements TenantEvent {
}
