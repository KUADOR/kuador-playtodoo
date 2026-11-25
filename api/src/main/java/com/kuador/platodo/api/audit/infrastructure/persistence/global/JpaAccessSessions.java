package com.kuador.platodo.api.audit.infrastructure.persistence.global;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.domain.AccessSession;

import jakarta.persistence.EntityManager;

/** Access sessions through the global persistence unit; writes need a transaction of that unit. */
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
}
