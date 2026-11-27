package com.kuador.platodo.api.audit.infrastructure.persistence.global;

import java.time.Instant;

import com.kuador.platodo.api.audit.application.port.out.ExpiredAccessAudit;

import jakarta.persistence.EntityManager;

/**
 * Bulk deletes through the global persistence unit. Events go first because they refer to their session; a session
 * that expired before the cutoff but still has a newer event is kept until a later run deletes that event.
 */
public final class JpaExpiredAccessAudit implements ExpiredAccessAudit {

    private static final String DELETE_EVENTS = "DELETE FROM AccessAuditEventEntity e WHERE e.occurredAt < :cutoff";
    private static final String DELETE_SESSIONS = """
            DELETE FROM AccessSessionEntity s
            WHERE s.expiresAt < :cutoff
              AND NOT EXISTS (SELECT 1 FROM AccessAuditEventEntity e WHERE e.sessionId = s.id)
            """;

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaExpiredAccessAudit(EntityManager global) {
        this.global = global;
    }

    @Override
    public int deleteBefore(Instant cutoff) {
        return delete(DELETE_EVENTS, cutoff) + delete(DELETE_SESSIONS, cutoff);
    }

    private int delete(String statement, Instant cutoff) {
        return global.createQuery(statement).setParameter("cutoff", cutoff).executeUpdate();
    }
}
