package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.subscription} (migration V8). {@code planSnapshot} is the jsonb object as text. */
@Entity
@Table(name = "subscription")
class SubscriptionEntity {

    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(name = "plan_id", nullable = false)
    private UUID planId;
    @ColumnTransformer(write = "CAST(? AS jsonb)")
    @Column(name = "plan_snapshot", nullable = false)
    private String planSnapshot;
    @Column(nullable = false)
    private String status;
    @Column(name = "status_since", nullable = false)
    private Instant statusSince;
    @Column(name = "status_changed_by", nullable = false)
    private UUID statusChangedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private long version;

    protected SubscriptionEntity() {
    }

    SubscriptionEntity(UUID id, UUID tenantId, UUID planId, String planSnapshot, String status, Instant statusSince,
            UUID statusChangedBy, Instant createdAt, long version) {
        this.id = id;
        this.tenantId = tenantId;
        this.planId = planId;
        this.planSnapshot = planSnapshot;
        this.status = status;
        this.statusSince = statusSince;
        this.statusChangedBy = statusChangedBy;
        this.createdAt = createdAt;
        this.version = version;
    }

    UUID id() {
        return id;
    }

    UUID tenantId() {
        return tenantId;
    }

    String planSnapshot() {
        return planSnapshot;
    }

    String status() {
        return status;
    }

    Instant statusSince() {
        return statusSince;
    }

    UUID statusChangedBy() {
        return statusChangedBy;
    }

    Instant createdAt() {
        return createdAt;
    }

    long version() {
        return version;
    }
}
