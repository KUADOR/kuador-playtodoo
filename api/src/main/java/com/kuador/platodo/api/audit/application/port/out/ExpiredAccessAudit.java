package com.kuador.platodo.api.audit.application.port.out;

import java.time.Instant;

/**
 * Deletion of the audit of the Kuador accesses past its retention (tables {@code access_audit_event} and
 * {@code access_session}, ADR-016).
 */
@FunctionalInterface
public interface ExpiredAccessAudit {

    /**
     * Deletes, of every complex, the audit events that occurred before {@code cutoff} and then the sessions that
     * expired before it and no remaining event refers to. Needs a transaction of the global schema.
     *
     * @return how many rows were deleted
     */
    int deleteBefore(Instant cutoff);
}
