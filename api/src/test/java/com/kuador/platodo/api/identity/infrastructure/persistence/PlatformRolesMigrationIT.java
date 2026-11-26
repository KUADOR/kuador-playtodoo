package com.kuador.platodo.api.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.support.TenantDatabase;

/**
 * Global migration V6 over data written by V3 (PLATODO-82): the PLATFORM_OWNER of PLATODO-26 keeps its access as
 * SUPER_ADMIN, and every operator capability starts disabled. Runs the global set in a scratch schema so the shared
 * global schema of the other tests is untouched.
 */
class PlatformRolesMigrationIT {

    private static final String GLOBAL_MIGRATIONS = "classpath:db/migration/global";
    private static final MigrationVersion BEFORE_ROLES = MigrationVersion.fromVersion("5");

    private String schema;
    private DataSource dataSource;
    private JdbcClient database;

    @BeforeEach
    void migrateUpToTheVersionBeforeRoles() {
        schema = "roles_migration_" + UUID.randomUUID().toString().replace("-", "");
        dataSource = TenantDatabase.tenantDataSource(schema);
        database = JdbcClient.create(dataSource);
        flyway().target(BEFORE_ROLES).load().migrate();
    }

    @AfterEach
    void dropScratchSchema() {
        TenantDatabase.admin().sql("DROP SCHEMA IF EXISTS \"%s\" CASCADE".formatted(schema)).update();
    }

    @Test
    void thePlatformOwnerBecomesSuperAdminAndKeepsItsAccess() {
        UUID ownerId = UUID.randomUUID();
        database.sql("""
                        INSERT INTO platform_user (id, email, password_hash, role, status, created_at)
                        VALUES (:id, 'sam.owner@example.com', 'hash', 'PLATFORM_OWNER', 'ACTIVE', now())
                        """)
                .param("id", ownerId)
                .update();

        flyway().load().migrate();

        Map<String, Object> owner = database.sql("""
                        SELECT role, status, display_name, password_hash FROM platform_user WHERE id = :id
                        """)
                .param("id", ownerId)
                .query()
                .singleRow();
        assertThat(owner).containsEntry("role", "SUPER_ADMIN")
                .containsEntry("status", "ACTIVE")
                .containsEntry("display_name", "sam.owner")
                .containsEntry("password_hash", "hash");
        assertThat(AuthenticatedPrincipal.global(ownerId, AccountType.PLATFORM, (String) owner.get("role"))
                .isSuperAdmin()).isTrue();
    }

    @Test
    void everyCapabilityIsSeededDisabledAtVersionZero() {
        flyway().load().migrate();

        List<Map<String, Object>> rows = database.sql("""
                        SELECT capability_key, enabled, version FROM operator_capability ORDER BY capability_key
                        """)
                .query()
                .listOfRows();
        assertThat(rows).extracting(row -> row.get("capability_key")).containsExactly(
                "COMPLEX_CREATE", "SUBSCRIPTION_PAYMENT_RECORD", "SUBSCRIPTION_STATUS_CHANGE", "TENANT_DATA_ACCESS");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("enabled")).isEqualTo(false);
            assertThat(row.get("version")).isEqualTo(0L);
        });
    }

    @Test
    void theOldRoleIsNoLongerAccepted() {
        flyway().load().migrate();

        assertThatThrownBy(() -> database.sql("""
                        INSERT INTO platform_user (id, email, password_hash, role, status, display_name, created_at)
                        VALUES (:id, 'legacy@example.com', 'hash', 'PLATFORM_OWNER', 'ACTIVE', 'Legacy', now())
                        """)
                .param("id", UUID.randomUUID())
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations(GLOBAL_MIGRATIONS);
    }
}
