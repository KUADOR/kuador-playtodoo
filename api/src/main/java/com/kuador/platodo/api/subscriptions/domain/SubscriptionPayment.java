package com.kuador.platodo.api.subscriptions.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A subscription payment received outside the platform and recorded by hand (ADR-004). Immutable: it is never edited
 * nor deleted. Its currency is the one of the plan snapshot the subscription had when it was recorded.
 *
 * @param tenantId   the complex, by its tenant id
 * @param recordedBy the Kuador team member who recorded it
 */
public record SubscriptionPayment(UUID id, UUID tenantId, BigDecimal amount, String currency, LocalDate paidOn,
        PaymentMethod method, UUID recordedBy, Instant recordedAt) {

    public SubscriptionPayment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(paidOn, "paidOn");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(recordedBy, "recordedBy");
        Objects.requireNonNull(recordedAt, "recordedAt");
    }

    /** The payment of {@code subscription} with {@code terms}, in the currency of its plan snapshot. */
    public static SubscriptionPayment record(UUID id, Subscription subscription, PaymentTerms terms, UUID recordedBy,
            Instant recordedAt) {
        return new SubscriptionPayment(id, subscription.tenantId(), terms.amount(), subscription.plan().currency(),
                terms.paidOn(), terms.method(), recordedBy, recordedAt);
    }
}
