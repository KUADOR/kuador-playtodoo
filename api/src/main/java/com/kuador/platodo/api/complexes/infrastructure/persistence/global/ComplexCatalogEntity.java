package com.kuador.platodo.api.complexes.infrastructure.persistence.global;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.complexes.domain.CatalogEntry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.complex_catalog} (migration V10). */
@Entity
@Table(name = "complex_catalog")
class ComplexCatalogEntity {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(nullable = false)
    private String subdomain;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(nullable = false)
    private boolean visible;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ComplexCatalogEntity() {
    }

    ComplexCatalogEntity(CatalogEntry entry, Instant at) {
        this.tenantId = entry.tenantId();
        show(entry, at);
    }

    void show(CatalogEntry entry, Instant at) {
        this.subdomain = entry.subdomain();
        this.displayName = entry.displayName();
        this.visible = true;
        this.updatedAt = at;
    }

    void hide(Instant at) {
        this.visible = false;
        this.updatedAt = at;
    }

    void changeSubdomain(String newSubdomain, Instant at) {
        this.subdomain = newSubdomain;
        this.updatedAt = at;
    }
}
