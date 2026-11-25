package com.kuador.platodo.api.tenancy.application.port.in;

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
     * What Tenancy tells about a complex.
     *
     * @param host host of its current subdomain ({@code <subdomain>.<root domain>}); empty once it has none (purge)
     */
    record TenantEntry(TenantId id, TenantStatus status, Optional<String> host) {
    }
}
