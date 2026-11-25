package com.kuador.platodo.api.tenancy.application.port.out;

import java.util.Optional;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** Directory of the complexes by subdomain, kept in the global schema. Never touches a tenant schema. */
public interface TenantDomains {

    /** The complex whose current subdomain is {@code subdomain}; empty for unknown or retired subdomains. */
    Optional<DomainOwner> findByCurrentSubdomain(Subdomain subdomain);

    /** The complex a subdomain points to, with what resolution needs to know about it. */
    record DomainOwner(TenantId tenantId, String displayName, TenantStatus status) {
    }
}
