package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.kuador.platodo.api.subscriptions.application.port.out.LastPayments;
import com.kuador.platodo.api.subscriptions.domain.LastPayment;

import jakarta.persistence.EntityManager;

/**
 * Latest payment per complex in one query: a payment is the latest when no other payment of its complex is later by
 * {@code paidOn}, then {@code recordedAt}, then id (so ties still give one row per complex). Served by the index
 * {@code subscription_payment_latest_idx}.
 */
public final class JpaLastPayments implements LastPayments {

    private static final String LATEST = """
            SELECT p FROM SubscriptionPaymentEntity p
            WHERE p.tenantId IN :tenantIds
              AND NOT EXISTS (
                SELECT 1 FROM SubscriptionPaymentEntity later
                WHERE later.tenantId = p.tenantId
                  AND (later.paidOn > p.paidOn
                       OR (later.paidOn = p.paidOn AND later.recordedAt > p.recordedAt)
                       OR (later.paidOn = p.paidOn AND later.recordedAt = p.recordedAt AND later.id > p.id)))
            """;

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaLastPayments(EntityManager global) {
        this.global = global;
    }

    @Override
    public Map<UUID, LastPayment> latestOf(Collection<UUID> tenantIds) {
        if (tenantIds.isEmpty()) {
            return Map.of();
        }
        return global.createQuery(LATEST, SubscriptionPaymentEntity.class)
                .setParameter("tenantIds", tenantIds)
                .getResultList().stream()
                .collect(Collectors.toMap(SubscriptionPaymentEntity::tenantId,
                        row -> new LastPayment(row.amount(), row.currency(), row.paidOn())));
    }
}
