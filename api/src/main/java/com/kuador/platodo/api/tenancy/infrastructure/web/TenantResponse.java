package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.Tenant;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** {@code Tenant} of the contract. Every property is always present, null when it does not apply. */
record TenantResponse(
        String id,
        String displayName,
        String subdomain,
        TenantStatus status,
        Instant createdAt,
        Instant activatedAt,
        Instant suspendedAt,
        Instant archivedAt,
        Integer retentionDays,
        Instant purgeDueAt,
        Instant purgedAt) {

    static TenantResponse of(RegisteredTenant registered) {
        Tenant tenant = registered.tenant();
        Subdomain subdomain = registered.subdomain();
        return new TenantResponse(tenant.id().value(), tenant.displayName(),
                subdomain == null ? null : subdomain.value(), tenant.status(), tenant.createdAt(),
                tenant.activatedAt(), tenant.suspendedAt(), tenant.archivedAt(), tenant.retentionDays(),
                tenant.purgeDueAt(), tenant.purgedAt());
    }
}
