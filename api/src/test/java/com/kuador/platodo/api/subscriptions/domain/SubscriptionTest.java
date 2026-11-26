package com.kuador.platodo.api.subscriptions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** ADR-014: a complex starts ACTIVE and its manual transitions follow the table of the contract. */
class SubscriptionTest {

    private static final Instant NOW = Instant.parse("2026-12-01T14:00:00Z");
    private static final UUID REGISTERED_BY = UUID.randomUUID();
    private static final PlanSnapshot PLAN = new PlanSnapshot(UUID.randomUUID(), "Premium", new BigDecimal("120.00"),
            "USD", "MONTHLY", 10, 25, Map.of(), NOW);

    @Test
    void aRegisteredComplexStartsActiveAtVersionZero() {
        Subscription subscription = Subscription.start(UUID.randomUUID(), UUID.randomUUID(), PLAN, REGISTERED_BY, NOW);

        assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.statusSince()).isEqualTo(NOW);
        assertThat(subscription.statusChangedBy()).isEqualTo(REGISTERED_BY);
        assertThat(subscription.version()).isZero();
    }

    @Test
    void manualTransitionsFollowTheContractTable() {
        assertThat(SubscriptionStatus.ACTIVE.manualTransitions()).containsExactlyInAnyOrder(
                SubscriptionStatus.EXPIRING, SubscriptionStatus.SUSPENDED, SubscriptionStatus.CANCELLED);
        assertThat(SubscriptionStatus.EXPIRING.manualTransitions()).containsExactlyInAnyOrder(
                SubscriptionStatus.ACTIVE, SubscriptionStatus.SUSPENDED, SubscriptionStatus.CANCELLED);
        assertThat(SubscriptionStatus.SUSPENDED.manualTransitions()).containsExactlyInAnyOrder(
                SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED);
        assertThat(SubscriptionStatus.CANCELLED.manualTransitions()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(SubscriptionStatus.class)
    void nothingIsAllowedWhileTheComplexDoesNotAcceptChanges(SubscriptionStatus status) {
        Subscription subscription = new Subscription(UUID.randomUUID(), UUID.randomUUID(), PLAN, status, NOW,
                REGISTERED_BY, NOW, 0);

        assertThat(subscription.allowedTransitions(false)).isEmpty();
        assertThat(subscription.allowedTransitions(true)).isEqualTo(status.manualTransitions());
    }
}
