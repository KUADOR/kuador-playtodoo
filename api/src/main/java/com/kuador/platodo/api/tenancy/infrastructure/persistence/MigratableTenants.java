package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.util.List;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Tenants whose schema must be up to date on startup: ACTIVE, SUSPENDED and ARCHIVED (ADR-009). */
@FunctionalInterface
public interface MigratableTenants {

    /** In a stable order, so every startup migrates tenants in the same sequence. */
    List<TenantId> list();
}
