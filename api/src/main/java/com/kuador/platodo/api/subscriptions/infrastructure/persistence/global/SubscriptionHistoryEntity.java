package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.SubscriptionHistoryEntry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.subscription_history} (migration V9). Entries are never changed once written. */
@Entity
@Table(name = "subscription_history")
class SubscriptionHistoryEntity {

    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(nullable = false, updatable = false)
    private String kind;
    @Column(updatable = false)
    private String before;
    @Column(nullable = false, updatable = false)
    private String after;
    @Column(name = "changed_by", nullable = false, updatable = false)
    private UUID changedBy;
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected SubscriptionHistoryEntity() {
    }

    SubscriptionHistoryEntity(SubscriptionHistoryEntry entry) {
        this.id = entry.id();
        this.tenantId = entry.tenantId();
        this.kind = entry.kind().name();
        this.before = entry.before();
        this.after = entry.after();
        this.changedBy = entry.changedBy();
        this.changedAt = entry.changedAt();
    }

    SubscriptionHistoryEntry toEntry() {
        return new SubscriptionHistoryEntry(id, tenantId, SubscriptionHistoryEntry.Kind.valueOf(kind), before, after,
                changedBy, changedAt);
    }
}
