package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.TenantDomains;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * Reads {@code global.tenant_domain} and {@code global.tenant}, owned by Tenancy. One indexed lookup per request
 * (primary key of {@code tenant_domain}); no cache until a measurement asks for one.
 */
public final class JdbcTenantDomains implements TenantDomains {

    private final JdbcClient global;

    /** {@code global} must use the shared pool: the lookup runs before any tenant scope is open. */
    public JdbcTenantDomains(JdbcClient global) {
        this.global = global;
    }

    @Override
    public Optional<DomainOwner> findByCurrentSubdomain(Subdomain subdomain) {
        return global.sql("""
                        SELECT t.id::text AS id, t.display_name, t.status
                        FROM global.tenant_domain d
                        JOIN global.tenant t ON t.id = d.tenant_id
                        WHERE d.subdomain = :subdomain AND d.is_current
                        """)
                .param("subdomain", subdomain.value())
                .query((row, number) -> new DomainOwner(
                        new TenantId(row.getString("id")),
                        row.getString("display_name"),
                        TenantStatus.valueOf(row.getString("status"))))
                .optional();
    }
}
