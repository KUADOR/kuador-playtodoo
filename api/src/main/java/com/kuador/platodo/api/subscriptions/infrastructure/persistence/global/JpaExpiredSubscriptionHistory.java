package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.time.Instant;

import com.kuador.platodo.api.subscriptions.application.port.out.ExpiredSubscriptionHistory;

import jakarta.persistence.EntityManager;

/** Bulk delete through the global persistence unit, served by {@code subscription_history_changed_at_idx}. */
public final class JpaExpiredSubscriptionHistory implements ExpiredSubscriptionHistory {

    private static final String DELETE_CHANGED_BEFORE =
            "DELETE FROM SubscriptionHistoryEntity h WHERE h.changedAt < :cutoff";

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaExpiredSubscriptionHistory(EntityManager global) {
        this.global = global;
    }

    @Override
    public int deleteChangedBefore(Instant cutoff) {
        return global.createQuery(DELETE_CHANGED_BEFORE).setParameter("cutoff", cutoff).executeUpdate();
    }
}
