package com.kuador.platodo.api.audit.application.port.out;

import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Ends the access sessions of one complex that are still open, kept in the global schema (ADR-015). */
@FunctionalInterface
public interface AccessSessionInvalidation {

    /**
     * Closes {@code at} every session of {@code tenant} that is neither closed nor expired by then; sessions of other
     * complexes are untouched. Needs a transaction of the global unit. Idempotent.
     *
     * @return how many sessions were closed
     */
    int closeOpenSessionsOf(TenantId tenant, Instant at);
}
