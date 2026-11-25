package com.kuador.platodo.api.tenancy.application.port.out;

import java.time.Instant;
import java.util.Optional;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.Tenant;

/**
 * The complexes and their subdomains in the global schema ({@code global.tenant} and {@code global.tenant_domain}).
 * Writes must run inside a {@link GlobalTransaction}. Never touches a tenant schema.
 */
public interface TenantRegistry {

    /**
     * Records a new complex with {@code subdomain} as its current subdomain.
     *
     * @throws com.kuador.platodo.api.tenancy.application.SubdomainTakenException if the subdomain is used, or was
     *         used, by a complex
     */
    void register(Tenant tenant, Subdomain subdomain);

    /** The complex with its current subdomain, without locking it. */
    Optional<RegisteredTenant> find(TenantId id);

    /** The complex with its current subdomain, locked until the surrounding {@link GlobalTransaction} ends. */
    Optional<RegisteredTenant> lock(TenantId id);

    /** Stores the lifecycle fields of a complex that is locked in the current transaction. */
    void save(Tenant tenant);

    /**
     * Retires the current subdomain of a locked complex and makes {@code next} its current one, both {@code at}.
     *
     * @throws com.kuador.platodo.api.tenancy.application.SubdomainTakenException if {@code next} is used, or was
     *         used, by a complex
     */
    void replaceSubdomain(RegisteredTenant tenant, Subdomain next, Instant at);

    /** A complex with its current subdomain; {@code subdomain} is null once the complex is purged. */
    record RegisteredTenant(Tenant tenant, Subdomain subdomain) {
    }
}
