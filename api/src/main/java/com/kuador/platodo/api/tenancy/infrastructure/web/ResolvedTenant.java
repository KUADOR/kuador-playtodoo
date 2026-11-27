package com.kuador.platodo.api.tenancy.infrastructure.web;

import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * The complex resolved for the current request, kept as request attribute {@value #REQUEST_ATTRIBUTE} by
 * {@link TenantResolutionFilter} so that Tenancy's own endpoints and guard do not look it up twice. Other modules use
 * {@code TenantScope}.
 *
 * @param status {@code ACTIVE}, or {@code SUSPENDED} or {@code ARCHIVED} for a request that announces a support
 *               session (ADR-020), which {@link SupportAccessGuard} then checks
 */
record ResolvedTenant(String displayName, TenantStatus status) {

    static final String REQUEST_ATTRIBUTE = "com.kuador.platodo.api.tenancy.ResolvedTenant";

    boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }
}
