package com.kuador.platodo.api.audit.infrastructure.events;

import org.springframework.context.event.EventListener;

import com.kuador.platodo.api.audit.application.InvalidateAccessSessionsOfPurgingTenant;
import com.kuador.platodo.api.tenancy.domain.event.TenantStatusChanged;

/**
 * Listens to {@link TenantStatusChanged} synchronously, inside the transaction that claims the purge (ADR-015): an
 * exception here rolls the claim back, so a complex is never {@code PURGING} with an open support session.
 */
public final class TenantPurgingAuditListener {

    private final InvalidateAccessSessionsOfPurgingTenant invalidateSessions;

    public TenantPurgingAuditListener(InvalidateAccessSessionsOfPurgingTenant invalidateSessions) {
        this.invalidateSessions = invalidateSessions;
    }

    @EventListener
    public void on(TenantStatusChanged changed) {
        invalidateSessions.on(changed);
    }
}
