package com.kuador.platodo.api.audit.infrastructure.persistence.global;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.access_session} (migration V4). */
@Entity
@Table(name = "access_session")
class AccessSessionEntity {

    @Id
    private UUID id;
    @Column(name = "platform_user_id", nullable = false, updatable = false)
    private UUID platformUserId;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(nullable = false)
    private String reason;
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(name = "closed_at")
    private Instant closedAt;

    protected AccessSessionEntity() {
    }

    static AccessSessionEntity of(AccessSession session) {
        AccessSessionEntity entity = new AccessSessionEntity();
        entity.id = session.id();
        entity.platformUserId = session.platformUserId();
        entity.tenantId = UUID.fromString(session.tenant().value());
        entity.reason = session.reason().value();
        entity.startedAt = session.startedAt();
        entity.expiresAt = session.expiresAt();
        entity.closedAt = session.closedAt().orElse(null);
        return entity;
    }

    AccessSession toSession() {
        return new AccessSession(id, platformUserId, new TenantId(tenantId.toString()), new AccessReason(reason),
                startedAt, expiresAt, Optional.ofNullable(closedAt));
    }

    void closeAt(Instant at) {
        closedAt = at;
    }
}
