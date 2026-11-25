package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Global and tenant migrations on startup (ADR-009): which tenants, in which order, and fail fast. */
@SpringBootTest
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class StartupMigrationsIT {

    private final JdbcClient admin = TenantDatabase.admin();

    @Autowired
    private TenantMigrations migrations;
    @Autowired
    private ScopedTenantDataSource tenantData;
    @Autowired
    private FlywayMigrationStrategy startupMigrations;

    @Test
    void startupRunsTheGlobalMigrationsAndThenTheTenantMigrations() {
        assertThat(startupMigrations).isInstanceOf(GlobalThenTenantMigrations.class);
        assertThat(tableExists("global", "tenant")).isTrue();
        assertThat(tableExists("global", "flyway_schema_history")).isTrue();
    }

    @Test
    void theNoTenantSchemaExistsAndHoldsNothing() {
        Integer relations = admin.sql("""
                        SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname = 'no_tenant'
                        """)
                .query(Integer.class).single();

        assertThat(schemaExists(TenantSearchPath.NO_TENANT_SCHEMA)).isTrue();
        assertThat(relations).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "SUSPENDED", "ARCHIVED"})
    void migratesTenantsWhoseDataIsKept(String status) {
        TenantId tenant = registeredTenant(status);

        migrations.migrateAll();

        assertThat(tableExists(TestTenants.schemaOf(tenant), "booking")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROVISIONING", "PURGING", "PURGED"})
    void skipsTenantsThatAreNotProvisionedOrAreBeingRemoved(String status) {
        TenantId tenant = registeredTenant(status);

        migrations.migrateAll();

        assertThat(schemaExists(TestTenants.schemaOf(tenant))).isFalse();
    }

    @Test
    void aFailingTenantStopsTheRunBeforeTheNextTenants() {
        TenantId broken = new TenantId(UUID.randomUUID().toString());
        TenantId next = new TenantId(UUID.randomUUID().toString());
        String brokenSchema = TestTenants.schemaOf(broken);
        admin.sql("CREATE SCHEMA " + brokenSchema).update();
        admin.sql("CREATE TABLE " + brokenSchema + ".stray (id int)").update();
        TenantMigrations failFast = new TenantMigrations(() -> List.of(broken, next), new TenantSchemaMigrator(tenantData));

        assertThatThrownBy(failFast::migrateAll)
                .isInstanceOf(TenantMigrationException.class)
                .hasMessageContaining(brokenSchema);
        assertThat(schemaExists(TestTenants.schemaOf(next))).isFalse();
    }

    @Test
    void aTenantSchemaNameMustDeriveFromItsId() {
        assertThatThrownBy(() -> admin.sql("""
                        INSERT INTO global.tenant (id, schema_name, display_name, status, created_at)
                        VALUES (:id, 't_00000000000040008000000000000000', 'Other', 'ACTIVE', now())
                        """)
                .param("id", UUID.randomUUID())
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private TenantId registeredTenant(String status) {
        TenantId tenant = new TenantId(UUID.randomUUID().toString());
        TestTenants.register(tenant, status);
        return tenant;
    }

    private boolean schemaExists(String schema) {
        return admin.sql("SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)")
                .param(schema).query(Boolean.class).single();
    }

    private boolean tableExists(String schema, String table) {
        return admin.sql("SELECT to_regclass(?) IS NOT NULL")
                .param(schema + "." + table).query(Boolean.class).single();
    }
}
