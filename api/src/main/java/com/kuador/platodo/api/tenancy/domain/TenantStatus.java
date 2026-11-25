package com.kuador.platodo.api.tenancy.domain;

/** Lifecycle state of a sports complex (ADR-013), as stored in {@code global.tenant.status}. */
public enum TenantStatus {
    PROVISIONING,
    ACTIVE,
    SUSPENDED,
    ARCHIVED,
    PURGING,
    PURGED
}
