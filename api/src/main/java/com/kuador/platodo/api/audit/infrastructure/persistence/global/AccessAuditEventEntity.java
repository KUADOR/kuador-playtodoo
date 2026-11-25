package com.kuador.platodo.api.audit.infrastructure.persistence.global;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.audit.domain.AccessOutcome;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Row of {@code global.access_audit_event} (migration V4). Inserted once; only {@link JpaAccessReasonRedaction}
 * changes it, when the complex is purged.
 */
@Entity
@Table(name = "access_audit_event")
class AccessAuditEventEntity {

    @Id
    private UUID id;
    @Column(name = "session_id", updatable = false)
    private UUID sessionId;
    @Column(name = "platform_user_id", nullable = false, updatable = false)
    private UUID platformUserId;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(updatable = false)
    private String reason;
    @Column(name = "http_method", nullable = false, updatable = false)
    private String httpMethod;
    @Column(name = "route_template", nullable = false, updatable = false)
    private String routeTemplate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AccessOutcome outcome;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
    @Column(name = "reason_redacted_at", updatable = false)
    private Instant reasonRedactedAt;

    protected AccessAuditEventEntity() {
    }

    static AccessAuditEventEntity of(AccessAuditEvent event) {
        AccessAuditEventEntity entity = new AccessAuditEventEntity();
        entity.id = event.id();
        entity.sessionId = event.sessionId().orElse(null);
        entity.platformUserId = event.platformUserId();
        entity.tenantId = UUID.fromString(event.tenant().value());
        entity.reason = event.reason().orElse(null);
        entity.httpMethod = event.httpMethod();
        entity.routeTemplate = event.routeTemplate();
        entity.outcome = event.outcome();
        entity.occurredAt = event.occurredAt();
        return entity;
    }

    AccessAuditEvent toEvent() {
        return new AccessAuditEvent(id, Optional.ofNullable(sessionId), platformUserId,
                new TenantId(tenantId.toString()), Optional.ofNullable(reason), reasonRedactedAt != null, httpMethod,
                routeTemplate, outcome, occurredAt);
    }
}
