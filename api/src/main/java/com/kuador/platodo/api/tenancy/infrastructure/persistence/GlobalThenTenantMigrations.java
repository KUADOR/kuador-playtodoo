package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;

/**
 * Startup migrations (ADR-009): first the global schema, which lists the tenants, then every migratable tenant
 * schema. Both run before the persistence units start, and any failure stops the startup.
 */
public final class GlobalThenTenantMigrations implements FlywayMigrationStrategy {

    private final TenantMigrations tenantMigrations;

    public GlobalThenTenantMigrations(TenantMigrations tenantMigrations) {
        this.tenantMigrations = tenantMigrations;
    }

    @Override
    public void migrate(Flyway globalMigrations) {
        globalMigrations.migrate();
        tenantMigrations.migrateAll();
    }
}
