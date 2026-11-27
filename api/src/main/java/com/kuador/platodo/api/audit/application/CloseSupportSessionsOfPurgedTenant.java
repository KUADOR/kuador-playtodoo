package com.kuador.platodo.api.audit.application;

import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.tenancy.domain.event.TenantPurged;

/**
 * Invalidates the support sessions still open on a purged complex (ADR-015, ADR-020). From {@code PURGING} on the host
 * of the complex already answers "not found", so no request reaches it; closing the sessions keeps their state true.
 * It runs inside the purge transaction, so a failure here leaves the complex not purged.
 */
public final class CloseSupportSessionsOfPurgedTenant {

    private final AccessSessions sessions;

    public CloseSupportSessionsOfPurgedTenant(AccessSessions sessions) {
        this.sessions = sessions;
    }

    public int close(TenantPurged purged) {
        return sessions.closeOpenOf(purged.tenantId(), purged.purgedAt());
    }
}
