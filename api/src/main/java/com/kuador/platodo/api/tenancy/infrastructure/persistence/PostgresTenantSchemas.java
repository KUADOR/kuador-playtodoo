package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.TenantSchemas;

/**
 * Provisions a tenant schema with the same Flyway migrations the startup applies (ADR-009) and drops it on the purge
 * (ADR-013). The dropped name is always a validated {@link TenantSchemaName} derived from the tenant id, never input.
 */
public final class PostgresTenantSchemas implements TenantSchemas {

    private final TenantMigrations migrations;
    private final JdbcClient global;

    /** {@code global} must use the shared pool, joined to the global transaction manager. */
    public PostgresTenantSchemas(TenantMigrations migrations, JdbcClient global) {
        this.migrations = migrations;
        this.global = global;
    }

    @Override
    public void provision(TenantId tenant) {
        migrations.migrate(tenant);
    }

    @Override
    public boolean exists(TenantId tenant) {
        return global.sql("SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = :schema)")
                .param("schema", TenantSchemaName.of(tenant).value())
                .query(Boolean.class)
                .single();
    }

    @Override
    public void dropIfExists(TenantId tenant) {
        TenantSchemaName schema = TenantSchemaName.of(tenant);
        global.sql("DROP SCHEMA IF EXISTS \"" + schema.value() + "\" CASCADE").update();
    }
}
