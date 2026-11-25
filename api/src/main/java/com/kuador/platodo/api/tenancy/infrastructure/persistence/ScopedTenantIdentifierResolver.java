package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.kuador.platodo.api.tenancy.application.port.in.NoTenantInScopeException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

/**
 * Tells Hibernate which tenant schema a new session uses: the one of the open {@link TenantScope}. Without a scope it
 * throws {@link NoTenantInScopeException}; it never falls back to a default tenant (ADR-010, D3).
 */
public final class ScopedTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantSchemaName.of(TenantScope.require()).value();
    }

    /** A current session must belong to the tenant of the open scope. */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
