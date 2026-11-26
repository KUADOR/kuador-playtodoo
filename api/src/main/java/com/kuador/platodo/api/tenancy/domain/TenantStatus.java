package com.kuador.platodo.api.tenancy.domain;

/**
 * Lifecycle state of a sports complex (ADR-013 as adjusted by ADR-014), as stored in {@code global.tenant.status}.
 * {@code PROVISIONING_FAILED} waits for a provisioning retry.
 */
public enum TenantStatus {
    PROVISIONING,
    PROVISIONING_FAILED,
    ACTIVE,
    SUSPENDED,
    ARCHIVED,
    PURGING,
    PURGED
}
