package com.kuador.platodo.api.tenancy.infrastructure.web;

/**
 * The complex resolved for the current request, kept as request attribute {@value #REQUEST_ATTRIBUTE} by
 * {@link TenantResolutionFilter} so that Tenancy's own endpoints do not look it up twice. Other modules use
 * {@code TenantScope}.
 */
record ActiveTenant(String displayName) {

    static final String REQUEST_ATTRIBUTE = "com.kuador.platodo.api.tenancy.ActiveTenant";
}
