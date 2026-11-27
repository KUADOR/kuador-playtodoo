package com.kuador.platodo.api.complexes.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A complex as the global catalog shows it (ADR-008): public data only, never read from its schema.
 *
 * @param tenantId the complex, by its tenant id
 */
public record CatalogEntry(UUID tenantId, String subdomain, String displayName) {

    public CatalogEntry {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(subdomain, "subdomain");
        Objects.requireNonNull(displayName, "displayName");
    }
}
