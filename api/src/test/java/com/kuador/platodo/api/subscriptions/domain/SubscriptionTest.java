package com.kuador.platodo.api.subscriptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * ADR-014: a complex starts ACTIVE, its manual transitions follow the table of the contract, and a plan change takes a
 * new snapshot unless the subscription is cancelled (PLATODO-27 CA5, CA6).
 */
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

    /** CA6: the status, when and by whom are replaced, and the version grows; the plan stays. */
    @Test
    void aManualStatusChangeRecordsWhenAndByWhom() {
        Subscription active = Subscription.start(UUID.randomUUID(), UUID.randomUUID(), PLAN, REGISTERED_BY, NOW);
        UUID changedBy = UUID.randomUUID();
        Instant later = NOW.plusSeconds(60);

        Subscription suspended = active.changeStatus(SubscriptionStatus.SUSPENDED, changedBy, later, true);

        assertThat(suspended.status()).isEqualTo(SubscriptionStatus.SUSPENDED);
        assertThat(suspended.statusSince()).isEqualTo(later);
        assertThat(suspended.statusChangedBy()).isEqualTo(changedBy);
        assertThat(suspended.version()).isEqualTo(1);
        assertThat(suspended.plan()).isEqualTo(PLAN);
        assertThat(suspended.createdAt()).isEqualTo(NOW);
    }

    @Test
    void cancelledIsTerminal() {
        Subscription cancelled = subscriptionIn(SubscriptionStatus.CANCELLED);

        for (SubscriptionStatus target : SubscriptionStatus.values()) {
            assertThatThrownBy(() -> cancelled.changeStatus(target, REGISTERED_BY, NOW, true))
                    .isInstanceOf(TransitionNotAllowedException.class);
        }
    }

    @Test
    void aTransitionOutsideTheTableOrToTheSameStatusIsNotAllowed() {
        assertThatThrownBy(() -> subscriptionIn(SubscriptionStatus.SUSPENDED)
                .changeStatus(SubscriptionStatus.EXPIRING, REGISTERED_BY, NOW, true))
                .isInstanceOf(TransitionNotAllowedException.class);
        assertThatThrownBy(() -> subscriptionIn(SubscriptionStatus.ACTIVE)
                .changeStatus(SubscriptionStatus.ACTIVE, REGISTERED_BY, NOW, true))
                .isInstanceOf(TransitionNotAllowedException.class);
    }

    @Test
    void noTransitionIsAllowedWhileTheComplexDoesNotAcceptChanges() {
        assertThatThrownBy(() -> subscriptionIn(SubscriptionStatus.ACTIVE)
                .changeStatus(SubscriptionStatus.SUSPENDED, REGISTERED_BY, NOW, false))
                .isInstanceOf(TransitionNotAllowedException.class);
    }

    /** CA5: the new snapshot replaces the old one; the status and the version follow. */
    @Test
    void aPlanChangeTakesTheNewSnapshotAndKeepsTheStatus() {
        Subscription expiring = subscriptionIn(SubscriptionStatus.EXPIRING);
        PlanSnapshot basic = new PlanSnapshot(UUID.randomUUID(), "Basic", new BigDecimal("50.00"), "USD", "MONTHLY",
                2, 5, Map.of(), NOW.plusSeconds(60));

        Subscription changed = expiring.changePlan(basic);

        assertThat(changed.plan()).isEqualTo(basic);
        assertThat(changed.status()).isEqualTo(SubscriptionStatus.EXPIRING);
        assertThat(changed.statusSince()).isEqualTo(expiring.statusSince());
        assertThat(changed.statusChangedBy()).isEqualTo(expiring.statusChangedBy());
        assertThat(changed.version()).isEqualTo(expiring.version() + 1);
    }

    @Test
    void theSamePlanIsRejected() {
        PlanSnapshot sameAgain = new PlanSnapshot(PLAN.planId(), "Premium renamed", new BigDecimal("130.00"), "USD",
                "MONTHLY", 10, 25, Map.of(), NOW.plusSeconds(60));

        assertThatThrownBy(() -> subscriptionIn(SubscriptionStatus.ACTIVE).changePlan(sameAgain))
                .isInstanceOf(SamePlanException.class);
    }

    @Test
    void aCancelledSubscriptionKeepsItsPlan() {
        PlanSnapshot basic = new PlanSnapshot(UUID.randomUUID(), "Basic", new BigDecimal("50.00"), "USD", "MONTHLY",
                2, 5, Map.of(), NOW);

        assertThatThrownBy(() -> subscriptionIn(SubscriptionStatus.CANCELLED).changePlan(basic))
                .isInstanceOf(SubscriptionCancelledException.class);
    }

    private static Subscription subscriptionIn(SubscriptionStatus status) {
        return new Subscription(UUID.randomUUID(), UUID.randomUUID(), PLAN, status, NOW, REGISTERED_BY, NOW, 3);
    }
}
