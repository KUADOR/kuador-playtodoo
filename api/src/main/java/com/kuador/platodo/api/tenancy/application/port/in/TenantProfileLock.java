package com.kuador.platodo.api.tenancy.application.port.in;

import java.util.Optional;

import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;

/**
 * Locks the registry row of a complex on the global host, so another module can compare the version of the complex
 * and change it without a concurrent lifecycle change slipping in between (optimistic concurrency of ADR-014). Reads
 * only the global directory, never a tenant schema.
 */
@FunctionalInterface
public interface TenantProfileLock {

    /**
     * The registry data of the complex, locked until the current global transaction ends; empty when there is none.
     * Must be called inside a global transaction.
     */
    Optional<TenantProfile> lock(TenantId tenant);
}
