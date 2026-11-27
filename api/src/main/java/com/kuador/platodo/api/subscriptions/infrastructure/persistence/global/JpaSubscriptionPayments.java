package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionPayments;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionPayment;

import jakarta.persistence.EntityManager;

/** Subscription payments through the global persistence unit; served by {@code subscription_payment_latest_idx}. */
public final class JpaSubscriptionPayments implements SubscriptionPayments {

    private static final String NEWEST_FIRST = """
            SELECT p FROM SubscriptionPaymentEntity p WHERE p.tenantId = :tenantId
            ORDER BY p.paidOn DESC, p.recordedAt DESC, p.id DESC
            """;
    private static final String COUNT = "SELECT count(p) FROM SubscriptionPaymentEntity p WHERE p.tenantId = :tenantId";

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaSubscriptionPayments(EntityManager global) {
        this.global = global;
    }

    @Override
    public void insert(SubscriptionPayment payment) {
        global.persist(new SubscriptionPaymentEntity(payment));
    }

    @Override
    public PaymentPage page(UUID tenantId, PageRequest page) {
        List<SubscriptionPayment> items = global.createQuery(NEWEST_FIRST, SubscriptionPaymentEntity.class)
                .setParameter("tenantId", tenantId)
                .setFirstResult(Math.toIntExact(page.offset()))
                .setMaxResults(page.size())
                .getResultList().stream()
                .map(SubscriptionPaymentEntity::toPayment)
                .toList();
        long total = global.createQuery(COUNT, Long.class).setParameter("tenantId", tenantId).getSingleResult();
        return new PaymentPage(items, total);
    }
}
