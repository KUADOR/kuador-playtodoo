package com.kuador.platodo.api.audit.infrastructure.persistence.global;

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

import com.kuador.platodo.api.support.TenantDatabase;

/**
 * Global migration V11 over audit written before it (PLATODO-88): every row is kept, denials get the code they were
 * answered with, and from then on a denial always carries a known code. Runs the global set in a scratch schema so
 * the shared global schema of the other tests is untouched.
 */
class SupportAuditMigrationIT {

    private static final String GLOBAL_MIGRATIONS = "classpath:db/migration/global";
    private static final MigrationVersion BEFORE_SUPPORT_AUDIT = MigrationVersion.fromVersion("10");

    private String schema;
    private DataSource dataSource;
    private JdbcClient database;
    private UUID platformUser;
    private UUID tenant;
    private UUID session;

    @BeforeEach
    void migrateUpToTheVersionBeforeSupportAuditWithAuditRows() {
        schema = "support_audit_migration_" + UUID.randomUUID().toString().replace("-", "");
        dataSource = TenantDatabase.tenantDataSource(schema);
        database = JdbcClient.create(dataSource);
        flyway().target(BEFORE_SUPPORT_AUDIT).load().migrate();
        platformUser = UUID.randomUUID();
        tenant = UUID.randomUUID();
        session = UUID.randomUUID();
        database.sql("""
                        INSERT INTO platform_user (id, email, password_hash, role, status, display_name, created_at)
                        VALUES (:id, 'sam@example.com', 'hash', 'SUPER_ADMIN', 'ACTIVE', 'Sam', now())
                        """).param("id", platformUser).update();
        database.sql("""
                        INSERT INTO tenant (id, schema_name, display_name, status, created_at)
                        VALUES (:id, :schema, 'Complex', 'ACTIVE', now())
                        """).param("id", tenant).param("schema", "t_" + tenant.toString().replace("-", "")).update();
        database.sql("""
                        INSERT INTO access_session (id, platform_user_id, tenant_id, reason, started_at, expires_at)
                        VALUES (:id, :user, :tenant, 'Ticket 1234', now(), now() + interval '30 minutes')
                        """).param("id", session).param("user", platformUser).param("tenant", tenant).update();
        database.sql("""
                        INSERT INTO access_audit_event (id, session_id, platform_user_id, tenant_id, reason,
                            http_method, route_template, outcome, occurred_at)
                        VALUES (gen_random_uuid(), :session, :user, :tenant, 'Ticket 1234', 'GET',
                                '/api/v1/staff/bookings', 'ALLOWED', now()),
                               (gen_random_uuid(), NULL, :user, :tenant, NULL, 'GET',
                                '/api/v1/staff/bookings', 'DENIED', now())
                        """).param("session", session).param("user", platformUser).param("tenant", tenant).update();
    }

    @AfterEach
    void dropScratchSchema() {
        TenantDatabase.admin().sql("DROP SCHEMA IF EXISTS \"%s\" CASCADE".formatted(schema)).update();
    }

    @Test
    void existingAuditIsKeptAndItsDenialsGetTheirCode() {
        flyway().load().migrate();

        List<Map<String, Object>> rows = database.sql("""
                        SELECT outcome, denial_code, operation_id, reason FROM access_audit_event ORDER BY outcome
                        """)
                .query()
                .listOfRows();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("outcome", "ALLOWED").containsEntry("denial_code", null)
                .containsEntry("operation_id", null).containsEntry("reason", "Ticket 1234");
        assertThat(rows.get(1)).containsEntry("outcome", "DENIED")
                .containsEntry("denial_code", "SUPPORT_SESSION_INVALID").containsEntry("operation_id", null);
    }

    @Test
    void aDenialWithoutAKnownCodeIsRejected() {
        flyway().load().migrate();

        assertThatThrownBy(() -> insertDenial(null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertDenial("ACCESS_SESSION_INVALID"))
                .isInstanceOf(DataIntegrityViolationException.class);
        insertDenial("TENANT_NOT_ACCESSIBLE");
    }

    private void insertDenial(String denialCode) {
        database.sql("""
                        INSERT INTO access_audit_event (id, platform_user_id, tenant_id, http_method, route_template,
                            operation_id, outcome, denial_code, occurred_at)
                        VALUES (gen_random_uuid(), :user, :tenant, 'POST',
                                '/api/v1/platform/complexes/{complexId}/support-sessions', 'openSupportSession',
                                'DENIED', :code, now())
                        """)
                .param("user", platformUser).param("tenant", tenant).param("code", denialCode)
                .update();
    }

    private FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations(GLOBAL_MIGRATIONS);
    }
}
