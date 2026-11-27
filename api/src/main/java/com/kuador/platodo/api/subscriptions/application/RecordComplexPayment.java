package com.kuador.platodo.api.subscriptions.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionPayments;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.domain.InvalidPaymentException;
import com.kuador.platodo.api.subscriptions.domain.PaymentDraft;
import com.kuador.platodo.api.subscriptions.domain.PaymentTerms;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionPayment;

/**
 * {@code POST /complexes/{complexId}/payments}: capability {@code SUBSCRIPTION_PAYMENT_RECORD} (ADR-017). Only records
 * a payment received outside the platform (ADR-004); nothing is charged. The subscription is locked so the currency
 * taken from its plan snapshot cannot change under a concurrent plan change.
 *
 * <p>"Today", the latest allowed {@code paidOn}, is the UTC date. [POR DEFINIR: time zone of the launch country]; in
 * the Americas the UTC date is never behind the local one, so a payment made today is never rejected.
 */
public final class RecordComplexPayment {

    private final CapabilityGuard guard;
    private final Subscriptions subscriptions;
    private final SubscriptionPayments payments;
    private final PlatformUserNames userNames;
    private final TransactionOperations globalTransaction;
    private final Clock clock;

    public RecordComplexPayment(CapabilityGuard guard, Subscriptions subscriptions, SubscriptionPayments payments,
            PlatformUserNames userNames, TransactionOperations globalTransaction, Clock clock) {
        this.guard = guard;
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.userNames = userNames;
        this.globalTransaction = globalTransaction;
        this.clock = clock;
    }

    /**
     * @throws ComplexNotFoundException if no complex has this id
     * @throws InvalidPaymentException  if a field is invalid
     */
    public RecordedPayment record(UUID complexId, PaymentDraft draft) {
        PlatformCaller caller = guard.require(Capability.SUBSCRIPTION_PAYMENT_RECORD);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        SubscriptionPayment payment = globalTransaction.execute(status -> {
            Subscription subscription = subscriptions.lockByTenant(complexId)
                    .orElseThrow(ComplexNotFoundException::new);
            PaymentTerms terms = draft.check(LocalDate.ofInstant(now, ZoneOffset.UTC));
            SubscriptionPayment recorded =
                    SubscriptionPayment.record(UUID.randomUUID(), subscription, terms, caller.id(), now);
            payments.insert(recorded);
            return recorded;
        });
        String recordedBy = userNames.displayNameOf(caller.id())
                .orElseThrow(() -> new IllegalStateException("Unknown platform user " + caller.id()));
        return new RecordedPayment(payment, recordedBy);
    }
}
