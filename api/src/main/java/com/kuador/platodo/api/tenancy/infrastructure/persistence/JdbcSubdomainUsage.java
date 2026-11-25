package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.tenancy.application.port.out.SubdomainUsage;
import com.kuador.platodo.api.tenancy.domain.Subdomain;

/** Reads {@code global.tenant_domain}: every row, current or retired, is a used subdomain. */
public final class JdbcSubdomainUsage implements SubdomainUsage {

    private final JdbcClient global;

    /** {@code global} must use the shared pool: lookups run outside any tenant scope. */
    public JdbcSubdomainUsage(JdbcClient global) {
        this.global = global;
    }

    @Override
    public Set<Subdomain> usedAmong(Collection<Subdomain> candidates) {
        if (candidates.isEmpty()) {
            return Set.of();
        }
        return global.sql("SELECT subdomain FROM global.tenant_domain WHERE subdomain IN (:candidates)")
                .param("candidates", candidates.stream().map(Subdomain::value).toList())
                .query(String.class)
                .stream()
                .map(Subdomain::new)
                .collect(Collectors.toUnmodifiableSet());
    }
}
