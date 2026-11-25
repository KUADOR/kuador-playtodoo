package com.kuador.platodo.api.audit.infrastructure.events;

import org.springframework.context.event.EventListener;

import com.kuador.platodo.api.audit.application.RedactAccessReasonsOfPurgedTenant;
import com.kuador.platodo.api.tenancy.domain.event.TenantPurged;

/**
 * Listens to {@link TenantPurged} synchronously, on the thread and inside the transaction of the purge (ADR-013,
 * D11), not after the commit: an exception here rolls the purge back.
 */
public final class TenantPurgedAuditListener {

    private final RedactAccessReasonsOfPurgedTenant redactReasons;

    public TenantPurgedAuditListener(RedactAccessReasonsOfPurgedTenant redactReasons) {
        this.redactReasons = redactReasons;
    }

    @EventListener
    public void on(TenantPurged purged) {
        redactReasons.redact(purged);
    }
}
