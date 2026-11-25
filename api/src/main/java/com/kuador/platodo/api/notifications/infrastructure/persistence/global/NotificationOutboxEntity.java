package com.kuador.platodo.api.notifications.infrastructure.persistence.global;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.notification_outbox} (migration V5). */
@Entity
@Table(name = "notification_outbox")
class NotificationOutboxEntity {

    @Id
    private UUID id;
    @Column(nullable = false, updatable = false)
    private String type;
    @Column(nullable = false, updatable = false)
    private String recipient;
    @ColumnTransformer(write = "CAST(? AS jsonb)")
    @Column(nullable = false, updatable = false)
    private String payload;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "sent_at")
    private Instant sentAt;

    protected NotificationOutboxEntity() {
    }

    static NotificationOutboxEntity pending(String type, String recipient, String payloadJson, Instant createdAt) {
        NotificationOutboxEntity entity = new NotificationOutboxEntity();
        entity.id = UUID.randomUUID();
        entity.type = type;
        entity.recipient = recipient;
        entity.payload = payloadJson;
        entity.createdAt = createdAt;
        return entity;
    }
}
