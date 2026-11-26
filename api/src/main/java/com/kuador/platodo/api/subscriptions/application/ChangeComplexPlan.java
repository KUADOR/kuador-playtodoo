package com.kuador.platodo.api.subscriptions.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionEvents;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionHistory;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.domain.PlanNotPublishedException;
import com.kuador.platodo.api.subscriptions.domain.PlanSnapshot;
import com.kuador.platodo.api.subscriptions.domain.SamePlanException;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionCancelledException;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionHistoryEntry;
import com.kuador.platodo.api.subscriptions.domain.event.SubscriptionPlanChanged;

/**
 * {@code PUT /complexes/{complexId}/plan}: SUPER_ADMIN only (ADR-017). In one global transaction, with the complex
 * locked at the version the caller read, it assigns a new snapshot of the published plan (ADR-018), records a
 * {@code PLAN} history entry and publishes {@link SubscriptionPlanChanged}.
 */
public final class ChangeComplexPlan {

    private final CapabilityGuard guard;
    private final ComplexLocks locks;
    private final Plans plans;
    private final Subscriptions subscriptions;
    private final SubscriptionHistory history;
    private final SubscriptionEvents events;
    private final ComplexDetails details;
    private final TransactionOperations globalTransaction;
    private final Clock clock;

    public ChangeComplexPlan(CapabilityGuard guard, ComplexLocks locks, Plans plans, Subscriptions subscriptions,
            SubscriptionHistory history, SubscriptionEvents events, ComplexDetails details,
            TransactionOperations globalTransaction, Clock clock) {
        this.guard = guard;
        this.locks = locks;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.history = history;
        this.events = events;
        this.details = details;
        this.globalTransaction = globalTransaction;
        this.clock = clock;
    }

    /**
     * @throws ComplexNotFoundException       if no complex has this id
     * @throws StaleVersionException          if {@code expectedVersion} is not the current version of the complex
     * @throws PlanNotPublishedException      if the plan does not exist or is a draft
     * @throws SubscriptionCancelledException if the subscription is cancelled
     * @throws SamePlanException              if the complex already has this plan
     */
    public ComplexDetail change(UUID complexId, UUID planId, long expectedVersion) {
        PlatformCaller caller = guard.requireSuperAdmin();
        return globalTransaction.execute(status -> {
            Subscription current = locks.lockAt(complexId, expectedVersion).subscription();
            Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
            PlanSnapshot plan = plans.find(planId)
                    .map(found -> PlanSnapshot.of(found, now))
                    .orElseThrow(PlanNotPublishedException::new);
            Subscription changed = current.changePlan(plan);
            subscriptions.update(changed);
            history.append(List.of(SubscriptionHistoryEntry.ofPlanChange(current, changed, caller.id())));
            events.publish(new SubscriptionPlanChanged(
                    complexId, current.plan().planId(), plan.planId(), caller.id(), now));
            return details.of(complexId);
        });
    }
}
