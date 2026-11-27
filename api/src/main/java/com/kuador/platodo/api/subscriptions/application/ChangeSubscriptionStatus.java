package com.kuador.platodo.api.subscriptions.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionEvents;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionHistory;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionHistoryEntry;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionStatus;
import com.kuador.platodo.api.subscriptions.domain.TransitionNotAllowedException;
import com.kuador.platodo.api.subscriptions.domain.event.SubscriptionStatusChanged;

/**
 * {@code POST /complexes/{complexId}/status-changes}: capability {@code SUBSCRIPTION_STATUS_CHANGE} (ADR-014,
 * ADR-017). The only way a subscription status changes: always by hand, never by a scheduled process. In one global
 * transaction, with the complex locked at the version the caller read, it applies a transition of the table, records a
 * {@code STATUS} history entry and publishes {@link SubscriptionStatusChanged}, whose synchronous consumers (Tenancy)
 * commit or roll back with it. The answer is read after them, so it shows their effect.
 */
public final class ChangeSubscriptionStatus {

    private final CapabilityGuard guard;
    private final ComplexLocks locks;
    private final Subscriptions subscriptions;
    private final SubscriptionHistory history;
    private final SubscriptionEvents events;
    private final ComplexDetails details;
    private final TransactionOperations globalTransaction;
    private final Clock clock;

    public ChangeSubscriptionStatus(CapabilityGuard guard, ComplexLocks locks, Subscriptions subscriptions,
            SubscriptionHistory history, SubscriptionEvents events, ComplexDetails details,
            TransactionOperations globalTransaction, Clock clock) {
        this.guard = guard;
        this.locks = locks;
        this.subscriptions = subscriptions;
        this.history = history;
        this.events = events;
        this.details = details;
        this.globalTransaction = globalTransaction;
        this.clock = clock;
    }

    /**
     * @throws ComplexNotFoundException      if no complex has this id
     * @throws StaleVersionException         if {@code expectedVersion} is not the current version of the complex
     * @throws RetentionExpiredException     if {@code target} reactivates a suspended subscription whose retention is
     *                                       over
     * @throws TransitionNotAllowedException if {@code target} is not an allowed transition now
     */
    public ComplexDetail change(UUID complexId, SubscriptionStatus target, long expectedVersion) {
        PlatformCaller caller = guard.require(Capability.SUBSCRIPTION_STATUS_CHANGE);
        return globalTransaction.execute(status -> {
            LockedComplex complex = locks.lockAt(complexId, expectedVersion);
            Subscription current = complex.subscription();
            Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
            if (isReactivation(current, target) && ComplexDetails.retentionOver(complex.tenant(), now)) {
                throw new RetentionExpiredException();
            }
            Subscription changed = current.changeStatus(target, caller.id(), now,
                    ComplexDetails.acceptsCommercialChanges(complex.tenant(), now));
            subscriptions.update(changed);
            history.append(List.of(SubscriptionHistoryEntry.ofStatusChange(current, changed)));
            events.publish(new SubscriptionStatusChanged(complexId, current.status(), target, caller.id(), now));
            return details.of(complexId);
        });
    }

    private static boolean isReactivation(Subscription current, SubscriptionStatus target) {
        return current.status() == SubscriptionStatus.SUSPENDED && target == SubscriptionStatus.ACTIVE;
    }
}
