package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.tenancy.application.SubdomainTakenException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.Tenant;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * {@code global.tenant} and {@code global.tenant_domain}, owned by Tenancy. The primary key of {@code tenant_domain}
 * is the subdomain, so two complexes racing for one subdomain are serialized by PostgreSQL: the second insert waits
 * for the first one and fails with a unique violation if it commits (ADR-012).
 */
public final class JdbcTenantRegistry implements TenantRegistry {

    private static final String TENANT_COLUMNS = """
            t.id::text AS id, t.display_name, t.status, t.created_at, t.activated_at, t.suspended_at, t.archived_at,
            t.retention_days, t.purge_due_at, t.purged_at, d.subdomain""";

    private static final String FIND = """
            SELECT %s
            FROM global.tenant t
            LEFT JOIN global.tenant_domain d ON d.tenant_id = t.id AND d.is_current
            WHERE t.id = :id::uuid
            """.formatted(TENANT_COLUMNS);

    private static final String LOCK_TENANT_ROW = " FOR UPDATE OF t";
    private static final String LOCK_TENANT_ROW_UNLESS_HELD = " FOR UPDATE OF t SKIP LOCKED";

    private static final String IN_RETENTION_DUE = """
            SELECT %s
            FROM global.tenant t
            LEFT JOIN global.tenant_domain d ON d.tenant_id = t.id AND d.is_current
            WHERE t.status IN ('SUSPENDED', 'ARCHIVED') AND t.purge_due_at <= :now
            ORDER BY t.purge_due_at, t.id
            """.formatted(TENANT_COLUMNS);

    private final JdbcClient global;

    /** {@code global} must use the shared pool, joined to the global transaction manager. */
    public JdbcTenantRegistry(JdbcClient global) {
        this.global = global;
    }

    @Override
    public void register(Tenant tenant, Subdomain subdomain, String ownerEmail) {
        global.sql("""
                        INSERT INTO global.tenant (id, schema_name, display_name, status, created_at, owner_email)
                        VALUES (:id::uuid, :schema, :displayName, :status, :createdAt, :ownerEmail)
                        """)
                .param("id", tenant.id().value())
                .param("schema", TenantSchemaName.of(tenant.id()).value())
                .param("displayName", tenant.displayName())
                .param("status", tenant.status().name())
                .param("createdAt", toTimestamp(tenant.createdAt()))
                .param("ownerEmail", ownerEmail)
                .update();
        insertCurrentSubdomain(tenant.id(), subdomain, tenant.createdAt());
    }

    @Override
    public Optional<String> ownerEmailOf(TenantId id) {
        return global.sql("SELECT owner_email FROM global.tenant WHERE id = :id::uuid AND owner_email IS NOT NULL")
                .param("id", id.value())
                .query(String.class)
                .optional();
    }

    @Override
    public void redactOwnerEmail(TenantId id) {
        global.sql("UPDATE global.tenant SET owner_email = NULL WHERE id = :id::uuid")
                .param("id", id.value())
                .update();
    }

    @Override
    public Optional<RegisteredTenant> find(TenantId id) {
        return queryOne(FIND, id);
    }

    @Override
    public Optional<RegisteredTenant> lock(TenantId id) {
        return queryOne(FIND + LOCK_TENANT_ROW, id);
    }

    @Override
    public Optional<RegisteredTenant> tryLock(TenantId id) {
        return queryOne(FIND + LOCK_TENANT_ROW_UNLESS_HELD, id);
    }

    @Override
    public List<RegisteredTenant> lockInRetentionDueBy(Instant now) {
        return global.sql(IN_RETENTION_DUE + LOCK_TENANT_ROW_UNLESS_HELD)
                .param("now", toTimestamp(now))
                .query((row, number) -> registeredTenant(row))
                .list();
    }

    @Override
    public List<TenantId> purging() {
        return global.sql("SELECT id::text FROM global.tenant WHERE status = 'PURGING' ORDER BY purge_due_at, id")
                .query(String.class)
                .list()
                .stream()
                .map(TenantId::new)
                .toList();
    }

    @Override
    public void save(Tenant tenant) {
        global.sql("""
                        UPDATE global.tenant
                        SET status = :status, activated_at = :activatedAt, suspended_at = :suspendedAt,
                            archived_at = :archivedAt, retention_days = :retentionDays, purge_due_at = :purgeDueAt,
                            purged_at = :purgedAt, version = version + 1
                        WHERE id = :id::uuid
                        """)
                .param("id", tenant.id().value())
                .param("status", tenant.status().name())
                .param("activatedAt", toTimestamp(tenant.activatedAt()))
                .param("suspendedAt", toTimestamp(tenant.suspendedAt()))
                .param("archivedAt", toTimestamp(tenant.archivedAt()))
                .param("retentionDays", tenant.retentionDays())
                .param("purgeDueAt", toTimestamp(tenant.purgeDueAt()))
                .param("purgedAt", toTimestamp(tenant.purgedAt()))
                .update();
    }

    @Override
    public void replaceSubdomain(RegisteredTenant tenant, Subdomain next, Instant at) {
        retireCurrentSubdomain(tenant.tenant().id(), at);
        insertCurrentSubdomain(tenant.tenant().id(), next, at);
    }

    @Override
    public void retireCurrentSubdomain(TenantId id, Instant at) {
        global.sql("""
                        UPDATE global.tenant_domain SET is_current = false, retired_at = :at
                        WHERE tenant_id = :tenantId::uuid AND is_current
                        """)
                .param("tenantId", id.value())
                .param("at", toTimestamp(at))
                .update();
    }

    /** The only unique key a new current subdomain can break is the subdomain itself: the old one is retired. */
    private void insertCurrentSubdomain(TenantId tenant, Subdomain subdomain, Instant at) {
        try {
            global.sql("""
                            INSERT INTO global.tenant_domain (subdomain, tenant_id, is_current, created_at)
                            VALUES (:subdomain, :tenantId::uuid, true, :at)
                            """)
                    .param("subdomain", subdomain.value())
                    .param("tenantId", tenant.value())
                    .param("at", toTimestamp(at))
                    .update();
        } catch (DuplicateKeyException usedOrRetired) {
            throw new SubdomainTakenException(usedOrRetired);
        }
    }

    private Optional<RegisteredTenant> queryOne(String sql, TenantId id) {
        return global.sql(sql)
                .param("id", id.value())
                .query((row, number) -> registeredTenant(row))
                .optional();
    }

    private static RegisteredTenant registeredTenant(ResultSet row) throws SQLException {
        Tenant tenant = new Tenant(
                new TenantId(row.getString("id")),
                row.getString("display_name"),
                TenantStatus.valueOf(row.getString("status")),
                instant(row, "created_at"),
                instant(row, "activated_at"),
                instant(row, "suspended_at"),
                instant(row, "archived_at"),
                row.getObject("retention_days", Integer.class),
                instant(row, "purge_due_at"),
                instant(row, "purged_at"));
        String subdomain = row.getString("subdomain");
        return new RegisteredTenant(tenant, subdomain == null ? null : new Subdomain(subdomain));
    }

    /** PgJDBC binds {@link OffsetDateTime} as timestamptz; {@link Instant} is not a supported JDBC type. */
    private static OffsetDateTime toTimestamp(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
