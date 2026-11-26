package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.operator_capability} (migration V6): whether operators have one capability. */
@Entity
@Table(name = "operator_capability")
class OperatorCapabilityEntity {

    @Id
    @Column(name = "capability_key")
    private String capabilityKey;
    @Column(nullable = false)
    private boolean enabled;
    @Column(nullable = false)
    private long version;

    protected OperatorCapabilityEntity() {
    }

    String capabilityKey() {
        return capabilityKey;
    }

    boolean enabled() {
        return enabled;
    }

    long version() {
        return version;
    }

    void change(boolean newEnabled, long newVersion) {
        this.enabled = newEnabled;
        this.version = newVersion;
    }
}
