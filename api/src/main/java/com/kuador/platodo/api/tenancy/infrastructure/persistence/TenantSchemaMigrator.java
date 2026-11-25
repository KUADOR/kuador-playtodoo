package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

/**
 * Applies the tenant migration set to one tenant schema (ADR-009). Each schema keeps its own
 * {@code flyway_schema_history}, so a tenant never sees another tenant's history.
 * The data source search_path must include the shared {@code extensions} schema (btree_gist).
 */
public class TenantSchemaMigrator {

    static final String TENANT_MIGRATIONS = "classpath:db/migration/tenant";

    private final DataSource dataSource;

    public TenantSchemaMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Creates the schema if needed and applies the pending tenant migrations; fails on the first error. */
    public void migrate(String tenantSchema) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(tenantSchema)
                .locations(TENANT_MIGRATIONS)
                .load()
                .migrate();
    }
}
