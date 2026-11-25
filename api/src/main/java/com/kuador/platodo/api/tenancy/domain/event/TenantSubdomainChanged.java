package com.kuador.platodo.api.tenancy.domain.event;

import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** The owner changed the subdomain; {@code previousSubdomain} no longer resolves and stays retired. */
public record TenantSubdomainChanged(TenantId tenantId, String previousSubdomain, String subdomain, Instant occurredAt)
        implements TenantEvent {
}
