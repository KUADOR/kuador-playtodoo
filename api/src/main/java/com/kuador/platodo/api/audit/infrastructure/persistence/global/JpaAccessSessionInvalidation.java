package com.kuador.platodo.api.audit.infrastructure.persistence.global;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.port.out.AccessSessionInvalidation;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import jakarta.persistence.EntityManager;

/** Bulk update through the global persistence unit; it fails without a transaction of that unit. */
public final class JpaAccessSessionInvalidation implements AccessSessionInvalidation {

    private static final String CLOSE_OPEN_SESSIONS = """
            UPDATE AccessSessionEntity s SET s.closedAt = :at
            WHERE s.tenantId = :tenant AND s.closedAt IS NULL AND s.expiresAt > :at
            """;

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaAccessSessionInvalidation(EntityManager global) {
        this.global = global;
    }

    @Override
    public int closeOpenSessionsOf(TenantId tenant, Instant at) {
        return global.createQuery(CLOSE_OPEN_SESSIONS)
                .setParameter("at", at)
                .setParameter("tenant", UUID.fromString(tenant.value()))
                .executeUpdate();
    }
}
