package com.kuador.platodo.api.complexes.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.complexes.application.port.out.ComplexCatalog;
import com.kuador.platodo.api.complexes.domain.CatalogEntry;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * Keeps the global catalog aligned with the lifecycle of each complex (ADR-014): only an {@code ACTIVE} complex is
 * visible, so a suspended or cancelled one disappears for players in the same commit that blocks it, and a purged one
 * loses its row. Runs inside the transaction of the lifecycle change; reads only the global directory, never a tenant
 * schema.
 */
public final class SyncComplexCatalog {

    private final ComplexCatalog catalog;
    private final TenantDirectory directory;

    public SyncComplexCatalog(ComplexCatalog catalog, TenantDirectory directory) {
        this.catalog = catalog;
        this.directory = directory;
    }

    public void statusChanged(TenantId tenant, TenantStatus status, Instant at) {
        UUID tenantId = UUID.fromString(tenant.value());
        if (status == TenantStatus.ACTIVE) {
            TenantProfile profile = directory.profiles(List.of(tenant)).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Active complex missing from the directory: "
                            + tenant));
            catalog.show(new CatalogEntry(tenantId, profile.subdomain(), profile.displayName()), at);
        } else {
            catalog.hide(tenantId, at);
        }
    }

    public void subdomainChanged(TenantId tenant, String subdomain, Instant at) {
        catalog.changeSubdomain(UUID.fromString(tenant.value()), subdomain, at);
    }

    /** A purged complex leaves the catalog for good: «borrar todo» includes its row (ADR-016). */
    public void purged(TenantId tenant) {
        catalog.remove(UUID.fromString(tenant.value()));
    }
}
