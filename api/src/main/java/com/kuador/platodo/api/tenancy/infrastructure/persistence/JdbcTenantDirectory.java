package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.tenancy.application.RequestHosts;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** Reads a complex and its current subdomain from {@code global.tenant} and {@code global.tenant_domain}. */
public final class JdbcTenantDirectory implements TenantDirectory {

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

    /** Tenant ids are UUIDs in the global schema; any other value names no complex. */
    private static Optional<UUID> uuidOf(TenantId tenant) {
        try {
            return Optional.of(UUID.fromString(tenant.value()));
        } catch (IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }
}
