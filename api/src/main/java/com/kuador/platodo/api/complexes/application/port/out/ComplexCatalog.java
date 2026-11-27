package com.kuador.platodo.api.complexes.application.port.out;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.complexes.domain.CatalogEntry;

/**
 * The global catalog of complexes ({@code global.complex_catalog}). Writes join the global transaction of the caller,
 * so they commit or roll back with the lifecycle change that triggers them.
 */
public interface ComplexCatalog {

    /** Lists {@code entry} as visible, creating its row if the complex had none. */
    void show(CatalogEntry entry, Instant at);

    /** Hides the complex; a complex without a row stays without one. */
    void hide(UUID tenantId, Instant at);

    /** Moves a listed complex to its new subdomain; a complex without a row stays without one. */
    void changeSubdomain(UUID tenantId, String subdomain, Instant at);

    /** Deletes the row of a purged complex (ADR-016); a complex without a row stays without one. */
    void remove(UUID tenantId);
}
