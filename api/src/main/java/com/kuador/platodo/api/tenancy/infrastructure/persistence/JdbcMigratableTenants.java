package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Reads the migratable tenants from {@code global.tenant}, owned by Tenancy. */
public final class JdbcMigratableTenants implements MigratableTenants {

    private final JdbcClient global;

    /** {@code global} must use the shared pool: this query runs outside any tenant scope. */
    public JdbcMigratableTenants(JdbcClient global) {
        this.global = global;
    }

    @Override
    public List<TenantId> list() {
        return global.sql("""
                        SELECT id::text FROM global.tenant
                        WHERE status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED')
                        ORDER BY created_at, id
                        """)
                .query(String.class)
                .list()
                .stream()
                .map(TenantId::new)
                .toList();
    }
}
