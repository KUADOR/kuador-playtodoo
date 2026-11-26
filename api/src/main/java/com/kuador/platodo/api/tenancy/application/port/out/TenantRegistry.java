package com.kuador.platodo.api.tenancy.application.port.out;

import java.time.Instant;
import java.util.List;
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
     * Records a new complex with {@code subdomain} as its current subdomain and the e-mail of its owner account.
     *
     * @throws com.kuador.platodo.api.tenancy.application.SubdomainTakenException if the subdomain is used, or was
     *         used, by a complex
     */
    void register(Tenant tenant, Subdomain subdomain, String ownerEmail);

    /** The owner e-mail given at registration; empty for complexes registered without one and once redacted. */
    Optional<String> ownerEmailOf(TenantId id);

    /** Redacts the owner e-mail of a locked complex, which is personal data (ADR-016). */
    void redactOwnerEmail(TenantId id);

    /** The complex with its current subdomain, without locking it. */
    Optional<RegisteredTenant> find(TenantId id);

    /** The complex with its current subdomain, locked until the surrounding {@link GlobalTransaction} ends. */
    Optional<RegisteredTenant> lock(TenantId id);

    /**
     * The complex with its current subdomain, locked until the surrounding {@link GlobalTransaction} ends; empty when
     * there is none or another transaction holds its lock, without waiting for it.
     */
    Optional<RegisteredTenant> tryLock(TenantId id);

    /**
     * The {@code ARCHIVED} complexes whose purge date is {@code now} or earlier, locked until the surrounding
     * {@link GlobalTransaction} ends. The ones another transaction holds are skipped, so several instances of the purge
     * job never claim the same complex.
     */
    List<RegisteredTenant> lockArchivedDueBy(Instant now);

    /** The complexes left in {@code PURGING}, whose purge still has to be done or retried. */
    List<TenantId> purging();

    /** Stores the lifecycle fields of a complex that is locked in the current transaction. */
    void save(Tenant tenant);

    /**
     * Retires the current subdomain of a locked complex and makes {@code next} its current one, both {@code at}.
     *
     * @throws com.kuador.platodo.api.tenancy.application.SubdomainTakenException if {@code next} is used, or was
     *         used, by a complex
     */
    void replaceSubdomain(RegisteredTenant tenant, Subdomain next, Instant at);

    /** Retires the current subdomain of a locked complex {@code at}; it stays used, so it is never reassigned. */
    void retireCurrentSubdomain(TenantId id, Instant at);

    /** A complex with its current subdomain; {@code subdomain} is null once the complex is purged. */
    record RegisteredTenant(Tenant tenant, Subdomain subdomain) {
    }
}
