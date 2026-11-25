package com.kuador.platodo.api.audit.application.port.out;

import java.time.Instant;

import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Anonymizes the free-text reasons of the accesses to one complex, kept in the global schema (ADR-013). */
@FunctionalInterface
public interface AccessReasonRedaction {

    /**
     * Replaces with {@code replacement}, and marks as redacted {@code at}, every reason of the access sessions and
     * audit events of {@code tenant} that is not redacted yet; rows of other complexes are untouched. Needs a
     * transaction of the global unit. Idempotent.
     *
     * @return how many rows changed
     */
    int redactReasonsOf(TenantId tenant, AccessReason replacement, Instant at);
}
