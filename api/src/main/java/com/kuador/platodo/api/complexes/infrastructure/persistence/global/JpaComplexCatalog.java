package com.kuador.platodo.api.complexes.infrastructure.persistence.global;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.complexes.application.port.out.ComplexCatalog;
import com.kuador.platodo.api.complexes.domain.CatalogEntry;

import jakarta.persistence.EntityManager;

/**
 * The catalog through the global persistence unit. Its callers run with the {@code tenant} row of the complex locked,
 * so two changes of one complex never race for its catalog row.
 */
public final class JpaComplexCatalog implements ComplexCatalog {

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaComplexCatalog(EntityManager global) {
        this.global = global;
    }

    @Override
    public void show(CatalogEntry entry, Instant at) {
        rowOf(entry.tenantId()).ifPresentOrElse(
                row -> row.show(entry, at),
                () -> global.persist(new ComplexCatalogEntity(entry, at)));
        global.flush();
    }

    @Override
    public void hide(UUID tenantId, Instant at) {
        rowOf(tenantId).ifPresent(row -> row.hide(at));
        global.flush();
    }

    @Override
    public void changeSubdomain(UUID tenantId, String subdomain, Instant at) {
        rowOf(tenantId).ifPresent(row -> row.changeSubdomain(subdomain, at));
        global.flush();
    }

    @Override
    public void remove(UUID tenantId) {
        rowOf(tenantId).ifPresent(global::remove);
        global.flush();
    }

    private Optional<ComplexCatalogEntity> rowOf(UUID tenantId) {
        return Optional.ofNullable(global.find(ComplexCatalogEntity.class, tenantId));
    }
}
