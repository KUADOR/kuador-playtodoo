package com.kuador.platodo.api.tenancy.application.port.out;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** The schema {@code t_<uuid>} of each complex (ADR-010). */
@FunctionalInterface
public interface TenantSchemas {

    /** Creates the schema of {@code tenant} if needed and applies the tenant migrations. Idempotent. */
    void provision(TenantId tenant);
}
