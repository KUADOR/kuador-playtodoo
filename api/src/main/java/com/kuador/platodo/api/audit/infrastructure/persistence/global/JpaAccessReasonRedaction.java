package com.kuador.platodo.api.audit.infrastructure.persistence.global;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.port.out.AccessReasonRedaction;
import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import jakarta.persistence.EntityManager;

/**
 * Bulk updates through the global persistence unit; they fail without a transaction of that unit. Denied audit events
 * carry no reason, so there is nothing to redact in them.
 */
public final class JpaAccessReasonRedaction implements AccessReasonRedaction {

    private static final String REDACT_SESSIONS = """
            UPDATE AccessSessionEntity s SET s.reason = :replacement, s.reasonRedactedAt = :at
            WHERE s.tenantId = :tenant AND s.reasonRedactedAt IS NULL
            """;
    private static final String REDACT_EVENTS = """
            UPDATE AccessAuditEventEntity e SET e.reason = :replacement, e.reasonRedactedAt = :at
            WHERE e.tenantId = :tenant AND e.reason IS NOT NULL AND e.reasonRedactedAt IS NULL
            """;

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaAccessReasonRedaction(EntityManager global) {
        this.global = global;
    }

    @Override
    public int redactReasonsOf(TenantId tenant, AccessReason replacement, Instant at) {
        UUID tenantId = UUID.fromString(tenant.value());
        return redact(REDACT_SESSIONS, tenantId, replacement, at) + redact(REDACT_EVENTS, tenantId, replacement, at);
    }

    private int redact(String update, UUID tenant, AccessReason replacement, Instant at) {
        return global.createQuery(update)
                .setParameter("replacement", replacement.value())
                .setParameter("at", at)
                .setParameter("tenant", tenant)
                .executeUpdate();
    }
}
