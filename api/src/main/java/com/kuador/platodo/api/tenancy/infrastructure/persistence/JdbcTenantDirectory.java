package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.tenancy.application.RequestHosts;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantProfileLock;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * Reads a complex and its current subdomain from {@code global.tenant} and {@code global.tenant_domain}, and locks its
 * {@code global.tenant} row for the global transaction of the caller (the shared pool is the data source of the global
 * transaction manager, so the lock joins that transaction).
 */
public final class JdbcTenantDirectory implements TenantDirectory, TenantProfileLock {

    private static final String PROFILE = """
            SELECT t.id::text AS id, t.display_name, t.status, d.subdomain, t.owner_email, t.created_at,
                   t.suspended_at, t.archived_at, t.retention_days, t.purge_due_at, t.purged_at, t.version
            FROM global.tenant t
            LEFT JOIN global.tenant_domain d ON d.tenant_id = t.id AND d.is_current
            """;

    private final JdbcClient global;
    private final RequestHosts hosts;

    /** {@code global} must use the shared pool: lookups run outside any tenant scope. */
    public JdbcTenantDirectory(JdbcClient global, RequestHosts hosts) {
        this.global = global;
        this.hosts = hosts;
    }

    @Override
    public Optional<TenantEntry> find(TenantId tenant) {
        Optional<UUID> id = uuidOf(tenant);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return global.sql("""
                        SELECT t.status, d.subdomain
                        FROM global.tenant t
                        LEFT JOIN global.tenant_domain d ON d.tenant_id = t.id AND d.is_current
                        WHERE t.id = :id
                        """)
                .param("id", id.get())
                .query((row, number) -> new TenantEntry(
                        tenant,
                        TenantStatus.valueOf(row.getString("status")),
                        Optional.ofNullable(row.getString("subdomain")).map(hosts::tenantHost)))
                .optional();
    }

    @Override
    public List<TenantProfile> profiles(Collection<TenantId> tenants) {
        List<UUID> ids = tenants.stream().map(JdbcTenantDirectory::uuidOf).flatMap(Optional::stream).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return global.sql(PROFILE + "WHERE t.id IN (:ids)")
                .param("ids", ids)
                .query((row, number) -> profileOf(row))
                .list();
    }

    @Override
    public Optional<TenantProfile> lock(TenantId tenant) {
        Optional<UUID> id = uuidOf(tenant);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return global.sql(PROFILE + "WHERE t.id = :id FOR UPDATE OF t")
                .param("id", id.get())
                .query((row, number) -> profileOf(row))
                .optional();
    }

    private static TenantProfile profileOf(ResultSet row) throws SQLException {
        return new TenantProfile(
                new TenantId(row.getString("id")),
                row.getString("display_name"),
                TenantStatus.valueOf(row.getString("status")),
                row.getString("subdomain"),
                row.getString("owner_email"),
                instant(row, "created_at"),
                instant(row, "suspended_at"),
                instant(row, "archived_at"),
                row.getObject("retention_days", Integer.class),
                instant(row, "purge_due_at"),
                instant(row, "purged_at"),
                row.getLong("version"));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    /** Tenant ids are UUIDs in the global schema; any other value names no complex. */
    private static Optional<UUID> uuidOf(TenantId tenant) {
        try {
            return Optional.of(UUID.fromString(tenant.value()));
        } catch (IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }
}
