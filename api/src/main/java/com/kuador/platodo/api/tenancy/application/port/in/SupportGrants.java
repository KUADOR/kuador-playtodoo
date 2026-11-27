package com.kuador.platodo.api.tenancy.application.port.in;

import java.util.UUID;

/**
 * Whether the current request on the host of a complex runs under a support session that Tenancy's guard checked and
 * audited (ADR-020). Modules whose operations Kuador support may use, such as the staff operations of Bookings, ask
 * it before treating a Kuador account as staff of the resolved complex.
 */
@FunctionalInterface
public interface SupportGrants {

    /** True only on the request the guard let through for {@code platformUserId} on the tenant in scope. */
    boolean isGrantedTo(UUID platformUserId);
}
