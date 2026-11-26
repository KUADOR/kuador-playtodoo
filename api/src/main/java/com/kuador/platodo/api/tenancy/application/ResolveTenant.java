package com.kuador.platodo.api.tenancy.application;

import com.kuador.platodo.api.tenancy.application.port.out.TenantDomains;
import com.kuador.platodo.api.tenancy.application.port.out.TenantDomains.DomainOwner;
import com.kuador.platodo.api.tenancy.domain.Subdomain;

/**
 * Resolves the complex of a tenant host from its subdomain label (ADR-012, D9 of the PLATODO-26 design). A malformed
 * label is rejected before any lookup; only the global directory is read, never a tenant schema.
 */
public final class ResolveTenant {

    private static final TenantResolution NOT_FOUND = new TenantResolution.NotFound();
    private static final TenantResolution UNAVAILABLE = new TenantResolution.Unavailable();

    private final TenantDomains domains;

    public ResolveTenant(TenantDomains domains) {
        this.domains = domains;
    }

    public TenantResolution resolve(String subdomainLabel) {
        return Subdomain.parse(subdomainLabel)
                .flatMap(domains::findByCurrentSubdomain)
                .map(ResolveTenant::byStatus)
                .orElse(NOT_FOUND);
    }

    private static TenantResolution byStatus(DomainOwner owner) {
        return switch (owner.status()) {
            case ACTIVE -> new TenantResolution.Active(owner.tenantId(), owner.displayName());
            case SUSPENDED, ARCHIVED -> UNAVAILABLE;
            case PROVISIONING, PROVISIONING_FAILED, PURGING, PURGED -> NOT_FOUND;
        };
    }
}
