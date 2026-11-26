package com.kuador.platodo.api.tenancy.application.port.in;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * Lets other modules look a complex up by id on the global host, where no tenant is resolved. Reads only the global
 * directory, never a tenant schema.
 */
public interface TenantDirectory {

    /** The complex with this id in any lifecycle state, purged ones included; empty when there is none. */
    Optional<TenantEntry> find(TenantId tenant);

    /**
     * The registry data of every complex among {@code tenants} that exists, in any lifecycle state, read in one query
     * whatever the number of ids (no N+1). Ids that name no complex are left out; the order is not specified.
     */
    List<TenantProfile> profiles(Collection<TenantId> tenants);

    /**
     * What Tenancy tells about a complex.
     *
     * @param host host of its current subdomain ({@code <subdomain>.<root domain>}); empty once it has none (purge)
     */
    record TenantEntry(TenantId id, TenantStatus status, Optional<String> host) {
    }

    /**
     * What the global registry keeps of a complex (ADR-013, ADR-014). Dates are null when they do not apply, like in
     * the lifecycle of the complex.
     *
     * @param subdomain     current subdomain; null once it has none (purge)
     * @param ownerEmail    owner e-mail given at registration; null when it was not kept or once redacted (purge)
     * @param retentionDays retention of the current archive; null unless archived
     * @param purgeDueAt    instant from which the purge may delete the schema; null unless archived
     * @param version       raised by every lifecycle change, for optimistic concurrency
     */
    record TenantProfile(
            TenantId id,
            String displayName,
            TenantStatus status,
            String subdomain,
            String ownerEmail,
            Instant createdAt,
            Instant suspendedAt,
            Instant archivedAt,
            Integer retentionDays,
            Instant purgeDueAt,
            Instant purgedAt,
            long version) {
    }
}
