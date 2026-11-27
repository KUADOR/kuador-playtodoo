package com.kuador.platodo.api.audit.infrastructure.persistence.global;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import jakarta.persistence.EntityManager;

/**
 * Audit trail through the global persistence unit. {@link #append} needs a transaction of that unit and flushes at
 * once, so a failing insert fails the caller before the audited request runs.
 */
public final class JpaAccessAuditLog implements AccessAuditLog {

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaAccessAuditLog(EntityManager global) {
        this.global = global;
    }

    @Override
    public void append(AccessAuditEvent event) {
        global.persist(AccessAuditEventEntity.of(event));
        global.flush();
    }

    @Override
    public AuditPage pageOf(TenantId tenant, Instant since, PageRequest page) {
        UUID tenantId = UUID.fromString(tenant.value());
        long total = global.createQuery("""
                        SELECT count(e) FROM AccessAuditEventEntity e
                        WHERE e.tenantId = :tenant AND e.occurredAt >= :since
                        """, Long.class)
                .setParameter("tenant", tenantId)
                .setParameter("since", since)
                .getSingleResult();
        List<AccessAuditEvent> items = global.createQuery("""
                        SELECT e FROM AccessAuditEventEntity e
                        WHERE e.tenantId = :tenant AND e.occurredAt >= :since
                        ORDER BY e.occurredAt DESC, e.id
                        """, AccessAuditEventEntity.class)
                .setParameter("tenant", tenantId)
                .setParameter("since", since)
                .setFirstResult(Math.toIntExact(page.offset()))
                .setMaxResults(page.size())
                .getResultList()
                .stream()
                .map(AccessAuditEventEntity::toEvent)
                .toList();
        return new AuditPage(items, total);
    }
}
