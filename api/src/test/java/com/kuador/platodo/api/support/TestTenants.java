package com.kuador.platodo.api.support;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantSchemaName;

/**
 * Two-tenant harness (ADR-010): tenants registered in {@code global.tenant} with a current subdomain in
 * {@code global.tenant_domain}, and migrated through the production {@link TenantMigrations}, until Tenancy
 * provisioning exists. Needs a started application context, which creates the global schema.
 */
public final class TestTenants {

    public static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    public static final TenantId B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");

    /** Bean that runs the startup migrations; beans that provision tenants while the context starts depend on it. */
    public static final String FLYWAY_INITIALIZER = "flywayInitializer";

    /** Display name of every registered test tenant. */
    public static final String DISPLAY_NAME = "Test complex";
    /** Root domain of the test environment ({@code playtodoo.root-domain}). */
    public static final String ROOT_DOMAIN = "localhost";
    /** Host of the global context. */
    public static final String GLOBAL_HOST = "api." + ROOT_DOMAIN;

    private static final String SUBDOMAIN_PREFIX = "test-";

    private TestTenants() {
    }

    /** Registers {@code tenant} as ACTIVE (once) and brings its schema up to date. */
    public static TenantId provision(TenantMigrations migrations, TenantId tenant) {
        register(tenant, "ACTIVE");
        migrations.migrate(tenant);
        return tenant;
    }

    /**
     * Registers {@code tenant} with {@code status} and its current subdomain {@link #subdomainOf}, unless it is already
     * registered; no schema is created.
     */
    public static void register(TenantId tenant, String status) {
        TenantDatabase.admin().sql("""
                        INSERT INTO global.tenant (id, schema_name, display_name, status, created_at)
                        VALUES (:id::uuid, :schema, :name, :status, now())
                        ON CONFLICT (id) DO NOTHING
                        """)
                .param("id", tenant.value())
                .param("schema", schemaOf(tenant))
                .param("name", DISPLAY_NAME)
                .param("status", status)
                .update();
        TenantDatabase.admin().sql("""
                        INSERT INTO global.tenant_domain (subdomain, tenant_id, is_current, created_at)
                        VALUES (:subdomain, :id::uuid, true, now())
                        ON CONFLICT (subdomain) DO NOTHING
                        """)
                .param("subdomain", subdomainOf(tenant))
                .param("id", tenant.value())
                .update();
    }

    /** Current subdomain of a registered test tenant, derived from its id so that tenants never share one. */
    public static String subdomainOf(TenantId tenant) {
        return SUBDOMAIN_PREFIX + tenant.value().replace("-", "");
    }

    /** Host of a registered test tenant. */
    public static String hostOf(TenantId tenant) {
        return subdomainOf(tenant) + "." + ROOT_DOMAIN;
    }

    public static String schemaOf(TenantId tenant) {
        return TenantSchemaName.of(tenant).value();
    }
}
