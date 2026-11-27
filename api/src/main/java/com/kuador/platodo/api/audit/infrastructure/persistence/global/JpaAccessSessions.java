package com.kuador.platodo.api.audit.infrastructure.persistence.global;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import jakarta.persistence.EntityManager;

/** Support sessions through the global persistence unit; writes need a transaction of that unit. */
public final class JpaAccessSessions implements AccessSessions {

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaAccessSessions(EntityManager global) {
        this.global = global;
    }

    @Override
    public void add(AccessSession session) {
        global.persist(AccessSessionEntity.of(session));
    }

    @Override
    public Optional<AccessSession> find(UUID sessionId) {
        return Optional.ofNullable(global.find(AccessSessionEntity.class, sessionId))
                .map(AccessSessionEntity::toSession);
    }

    @Override
    public void close(UUID sessionId, Instant closedAt) {
        Optional.ofNullable(global.find(AccessSessionEntity.class, sessionId))
                .ifPresent(session -> session.closeAt(closedAt));
    }

    @Override
    public int closeOpenOf(TenantId tenant, Instant closedAt) {
        return global.createQuery("""
                        UPDATE AccessSessionEntity s SET s.closedAt = :closedAt
                        WHERE s.tenantId = :tenant AND s.closedAt IS NULL AND s.expiresAt > :closedAt
                        """)
                .setParameter("closedAt", closedAt)
                .setParameter("tenant", UUID.fromString(tenant.value()))
                .executeUpdate();
    }
}
