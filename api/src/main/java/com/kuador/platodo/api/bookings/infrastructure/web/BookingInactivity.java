package com.kuador.platodo.api.bookings.infrastructure.web;

import java.util.EnumSet;
import java.util.Set;

import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * Whether the entries answered to staff are inactive (ADR-019): derived from the lifecycle status of the resolved
 * tenant, never stored, so suspending and reactivating a complex rewrite no booking row. Only Kuador support reaches
 * a suspended or archived complex (ADR-020); everyone else is stopped by the tenant filter before this point.
 */
final class BookingInactivity {

    private static final Set<TenantStatus> INACTIVE = EnumSet.of(TenantStatus.SUSPENDED, TenantStatus.ARCHIVED);

    private final TenantDirectory directory;

    BookingInactivity(TenantDirectory directory) {
        this.directory = directory;
    }

    /** One lookup in the global directory per request, whatever the number of entries answered. */
    boolean ofResolvedTenant() {
        TenantId tenant = TenantScope.require();
        TenantStatus status = directory.find(tenant)
                .orElseThrow(() -> new IllegalStateException("Resolved tenant missing from the directory: " + tenant))
                .status();
        return INACTIVE.contains(status);
    }
}
