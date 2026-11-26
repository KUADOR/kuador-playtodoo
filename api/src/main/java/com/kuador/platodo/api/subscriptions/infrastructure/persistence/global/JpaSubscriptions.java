package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionStatus;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

/** Subscriptions through the global persistence unit. {@code tenant_id} is unique: one subscription per complex. */
public final class JpaSubscriptions implements Subscriptions {

    private static final String BY_TENANT = "SELECT s FROM SubscriptionEntity s WHERE s.tenantId = :tenantId";
    private static final String NEWEST_FIRST =
            "SELECT s FROM SubscriptionEntity s ORDER BY s.createdAt DESC, s.tenantId";
    private static final String COUNT = "SELECT count(s) FROM SubscriptionEntity s";

    private final EntityManager global;
    private final PlanSnapshotJson snapshots;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaSubscriptions(EntityManager global, JsonMapper json) {
        this.global = global;
        this.snapshots = new PlanSnapshotJson(json);
    }

    @Override
    public void insert(Subscription subscription) {
        global.persist(new SubscriptionEntity(subscription.id(), subscription.tenantId(),
                subscription.plan().planId(), snapshots.write(subscription.plan()), subscription.status().name(),
                subscription.statusSince(), subscription.statusChangedBy(), subscription.createdAt(),
                subscription.version()));
    }

    @Override
    public Optional<Subscription> findByTenant(UUID tenantId) {
        return global.createQuery(BY_TENANT, SubscriptionEntity.class)
                .setParameter("tenantId", tenantId)
                .getResultList().stream()
                .findFirst()
                .map(this::toSubscription);
    }

    @Override
    public SubscriptionPage page(PageRequest page) {
        List<Subscription> items = global.createQuery(NEWEST_FIRST, SubscriptionEntity.class)
                .setFirstResult(Math.toIntExact(page.offset()))
                .setMaxResults(page.size())
                .getResultList().stream()
                .map(this::toSubscription)
                .toList();
        return new SubscriptionPage(items, global.createQuery(COUNT, Long.class).getSingleResult());
    }

    private Subscription toSubscription(SubscriptionEntity row) {
        return new Subscription(row.id(), row.tenantId(), snapshots.read(row.planSnapshot()),
                SubscriptionStatus.valueOf(row.status()), row.statusSince(), row.statusChangedBy(), row.createdAt(),
                row.version());
    }
}
