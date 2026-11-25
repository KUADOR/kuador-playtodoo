package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.TenantSchemas;

/** Provisions a tenant schema with the same Flyway migrations the startup applies (ADR-009). */
public final class MigratingTenantSchemas implements TenantSchemas {

    private final TenantMigrations migrations;

    public MigratingTenantSchemas(TenantMigrations migrations) {
        this.migrations = migrations;
    }

    @Override
    public void provision(TenantId tenant) {
        migrations.migrate(tenant);
    }
}
