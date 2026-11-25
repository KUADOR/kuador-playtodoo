package com.kuador.platodo.api.support;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantSchemaName;

/**
 * Two-tenant harness (ADR-010): tenants registered in {@code global.tenant} and migrated through the production
 * {@link TenantMigrations}, until Tenancy provisioning exists. Needs a started application context, which creates
 * the global schema.
 */
public final class TestTenants {

    public static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    public static final TenantId B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");

    /** Bean that runs the startup migrations; beans that provision tenants while the context starts depend on it. */
    public static final String FLYWAY_INITIALIZER = "flywayInitializer";

    private static final String TEST_DISPLAY_NAME = "Test complex";

    private TestTenants() {
    }

    /** Registers {@code tenant} as ACTIVE (once) and brings its schema up to date. */
    public static TenantId provision(TenantMigrations migrations, TenantId tenant) {
        register(tenant, "ACTIVE");
        migrations.migrate(tenant);
        return tenant;
    }

    /** Registers {@code tenant} with {@code status} unless it is already registered; no schema is created. */
    public static void register(TenantId tenant, String status) {
        TenantDatabase.admin().sql("""
                        INSERT INTO global.tenant (id, schema_name, display_name, status, created_at)
                        VALUES (:id::uuid, :schema, :name, :status, now())
                        ON CONFLICT (id) DO NOTHING
                        """)
                .param("id", tenant.value())
                .param("schema", schemaOf(tenant))
                .param("name", TEST_DISPLAY_NAME)
                .param("status", status)
                .update();
    }

    public static String schemaOf(TenantId tenant) {
        return TenantSchemaName.of(tenant).value();
    }
}
