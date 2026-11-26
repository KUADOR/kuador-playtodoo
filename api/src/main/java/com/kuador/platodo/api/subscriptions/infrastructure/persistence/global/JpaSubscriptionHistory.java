package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionHistory;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionHistoryEntry;

import jakarta.persistence.EntityManager;

/**
 * Subscription history through the global persistence unit; served by {@code subscription_history_newest_idx}. The
 * registration writes the plan entry before the status entry at the same instant; the id keeps the order stable.
 */
public final class JpaSubscriptionHistory implements SubscriptionHistory {

    private static final String NEWEST_FIRST = """
            SELECT h FROM SubscriptionHistoryEntity h WHERE h.tenantId = :tenantId AND h.changedAt >= :since
            ORDER BY h.changedAt DESC, h.id DESC
            """;
    private static final String COUNT = """
            SELECT count(h) FROM SubscriptionHistoryEntity h WHERE h.tenantId = :tenantId AND h.changedAt >= :since
            """;

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaSubscriptionHistory(EntityManager global) {
        this.global = global;
    }

    @Override
    public void append(List<SubscriptionHistoryEntry> entries) {
        entries.forEach(entry -> global.persist(new SubscriptionHistoryEntity(entry)));
    }

    @Override
    public HistoryPage page(UUID tenantId, Instant since, PageRequest page) {
        List<SubscriptionHistoryEntry> items = global.createQuery(NEWEST_FIRST, SubscriptionHistoryEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("since", since)
                .setFirstResult(Math.toIntExact(page.offset()))
                .setMaxResults(page.size())
                .getResultList().stream()
                .map(SubscriptionHistoryEntity::toEntry)
                .toList();
        long total = global.createQuery(COUNT, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("since", since)
                .getSingleResult();
        return new HistoryPage(items, total);
    }
}
