package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

/**
 * Applies the tenant migration set to tenant schemas, one tenant at a time and each inside its own
 * {@link TenantScope} (ADR-009). The first failure stops the run: startup fails fast instead of serving tenants
 * whose schemas are in a mixed state.
 */
public final class TenantMigrations {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrations.class);

    private final MigratableTenants tenants;
    private final TenantSchemaMigrator migrator;

    /** {@code migrator} must use a {@link ScopedTenantDataSource}, so it only reaches the scoped schema. */
    public TenantMigrations(MigratableTenants tenants, TenantSchemaMigrator migrator) {
        this.tenants = tenants;
        this.migrator = migrator;
    }

    /** Migrates every migratable tenant; throws {@link TenantMigrationException} on the first failure. */
    public void migrateAll() {
        tenants.list().forEach(this::migrate);
    }

    /** Creates the schema of {@code tenant} if needed and applies its pending migrations. */
    public void migrate(TenantId tenant) {
        TenantSchemaName schema = TenantSchemaName.of(tenant);
        try {
            TenantScope.run(tenant, () -> migrator.migrate(schema.value()));
        } catch (RuntimeException error) {
            throw new TenantMigrationException(schema, error);
        }
        log.info("Tenant schema {} is up to date", schema.value());
    }
}
