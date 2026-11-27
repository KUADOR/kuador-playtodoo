package com.kuador.platodo.api.complexes.infrastructure.events;

import org.springframework.context.event.EventListener;

import com.kuador.platodo.api.complexes.application.SyncComplexCatalog;
import com.kuador.platodo.api.tenancy.domain.event.TenantPurged;
import com.kuador.platodo.api.tenancy.domain.event.TenantStatusChanged;
import com.kuador.platodo.api.tenancy.domain.event.TenantSubdomainChanged;

/**
 * Listens to the lifecycle of Tenancy synchronously, on the thread and inside the transaction of the change (ADR-014),
 * not after the commit: an exception here rolls the change back, so the catalog never shows a blocked complex.
 */
public final class TenantCatalogListener {

    private final SyncComplexCatalog sync;

    public TenantCatalogListener(SyncComplexCatalog sync) {
        this.sync = sync;
    }

    @EventListener
    public void on(TenantStatusChanged changed) {
        sync.statusChanged(changed.tenantId(), changed.status(), changed.occurredAt());
    }

    @EventListener
    public void on(TenantSubdomainChanged changed) {
        sync.subdomainChanged(changed.tenantId(), changed.subdomain(), changed.occurredAt());
    }

    /** Inside the purge transaction (ADR-016): if the row cannot be deleted, the complex stays {@code PURGING}. */
    @EventListener
    public void on(TenantPurged purged) {
        sync.purged(purged.tenantId());
    }
}
