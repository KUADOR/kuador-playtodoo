package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Row of {@code global.plan_field_definition} (migration V7). {@code basicOrder} is only set for basic fields, which
 * the migration seeds; this code only inserts custom ones.
 */
@Entity
@Table(name = "plan_field_definition")
class PlanFieldDefinitionEntity {

    @Id
    private UUID id;
    @Column(nullable = false, updatable = false)
    private String key;
    @Column(nullable = false)
    private String label;
    @Column(nullable = false, updatable = false)
    private String type;
    @Column(nullable = false)
    private boolean required;
    @Column(nullable = false, updatable = false)
    private String origin;
    @Column(name = "basic_order", updatable = false)
    private Short basicOrder;
    @Column(name = "retired_at")
    private Instant retiredAt;
    @Column(nullable = false)
    private long version;

    protected PlanFieldDefinitionEntity() {
    }

    static PlanFieldDefinitionEntity custom(UUID id, String key, String type) {
        PlanFieldDefinitionEntity row = new PlanFieldDefinitionEntity();
        row.id = id;
        row.key = key;
        row.type = type;
        row.origin = "CUSTOM";
        return row;
    }

    void change(String newLabel, boolean newRequired, Instant newRetiredAt, long newVersion) {
        this.label = newLabel;
        this.required = newRequired;
        this.retiredAt = newRetiredAt;
        this.version = newVersion;
    }

    UUID id() {
        return id;
    }

    String key() {
        return key;
    }

    String label() {
        return label;
    }

    String type() {
        return type;
    }

    boolean required() {
        return required;
    }

    String origin() {
        return origin;
    }

    Instant retiredAt() {
        return retiredAt;
    }

    long version() {
        return version;
    }
}
