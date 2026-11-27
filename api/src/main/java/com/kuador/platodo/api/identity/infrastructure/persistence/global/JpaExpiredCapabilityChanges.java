package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.time.Instant;

import com.kuador.platodo.api.identity.application.port.out.ExpiredCapabilityChanges;

import jakarta.persistence.EntityManager;

/** Bulk delete through the global persistence unit, served by {@code capability_change_changed_at_idx}. */
public final class JpaExpiredCapabilityChanges implements ExpiredCapabilityChanges {

    private static final String DELETE_CHANGED_BEFORE =
            "DELETE FROM CapabilityChangeEntity c WHERE c.changedAt < :cutoff";

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaExpiredCapabilityChanges(EntityManager global) {
        this.global = global;
    }

    @Override
    public int deleteChangedBefore(Instant cutoff) {
        return global.createQuery(DELETE_CHANGED_BEFORE).setParameter("cutoff", cutoff).executeUpdate();
    }
}
