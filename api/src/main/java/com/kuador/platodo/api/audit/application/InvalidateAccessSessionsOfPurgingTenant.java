package com.kuador.platodo.api.audit.application;

import com.kuador.platodo.api.audit.application.port.out.AccessSessionInvalidation;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;
import com.kuador.platodo.api.tenancy.domain.event.TenantStatusChanged;

/**
 * A complex that enters {@code PURGING} loses its open support sessions in the same transaction (ADR-015, step 1), so
 * no session outlives the claim of its purge. Other status changes leave the sessions as they are.
 */
public final class InvalidateAccessSessionsOfPurgingTenant {

    private final AccessSessionInvalidation invalidation;

    public InvalidateAccessSessionsOfPurgingTenant(AccessSessionInvalidation invalidation) {
        this.invalidation = invalidation;
    }

    /** @return how many sessions were closed */
    public int on(TenantStatusChanged changed) {
        if (changed.status() != TenantStatus.PURGING) {
            return 0;
        }
        return invalidation.closeOpenSessionsOf(changed.tenantId(), changed.occurredAt());
    }
}
