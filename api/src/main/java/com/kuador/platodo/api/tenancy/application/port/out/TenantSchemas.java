package com.kuador.platodo.api.tenancy.application.port.out;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** The schema {@code t_<uuid>} of each complex (ADR-010). */
public interface TenantSchemas {

    /** Creates the schema of {@code tenant} if needed and applies the tenant migrations. Idempotent. */
    void provision(TenantId tenant);

    boolean exists(TenantId tenant);

    /**
     * Drops the schema of {@code tenant} with all its data, if it exists, inside the surrounding
     * {@link GlobalTransaction}: PostgreSQL rolls the drop back with it. Irreversible once committed. Idempotent.
     */
    void dropIfExists(TenantId tenant);
}
