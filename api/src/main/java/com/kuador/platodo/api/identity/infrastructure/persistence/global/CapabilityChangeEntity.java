package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.capability_change} (migration V6). Insert only. */
@Entity
@Table(name = "capability_change")
class CapabilityChangeEntity {

    @Id
    private UUID id;
    @Column(name = "capability_key", nullable = false, updatable = false)
    private String capabilityKey;
    @Column(nullable = false, updatable = false)
    private boolean enabled;
    @Column(name = "changed_by", nullable = false, updatable = false)
    private UUID changedBy;
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected CapabilityChangeEntity() {
    }

    CapabilityChangeEntity(UUID id, String capabilityKey, boolean enabled, UUID changedBy, Instant changedAt) {
        this.id = id;
        this.capabilityKey = capabilityKey;
        this.enabled = enabled;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    UUID id() {
        return id;
    }

    String capabilityKey() {
        return capabilityKey;
    }

    boolean enabled() {
        return enabled;
    }

    UUID changedBy() {
        return changedBy;
    }

    Instant changedAt() {
        return changedAt;
    }
}
