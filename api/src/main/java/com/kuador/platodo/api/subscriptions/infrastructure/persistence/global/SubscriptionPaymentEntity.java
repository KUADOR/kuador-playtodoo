package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.PaymentMethod;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionPayment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.subscription_payment} (migration V8). Payments are immutable: every column is insert-only. */
@Entity
@Table(name = "subscription_payment")
class SubscriptionPaymentEntity {

    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(nullable = false, updatable = false)
    private BigDecimal amount;
    @Column(nullable = false, updatable = false)
    private String currency;
    @Column(name = "paid_on", nullable = false, updatable = false)
    private LocalDate paidOn;
    @Column(nullable = false, updatable = false)
    private String method;
    @Column(name = "recorded_by", nullable = false, updatable = false)
    private UUID recordedBy;
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected SubscriptionPaymentEntity() {
    }

    SubscriptionPaymentEntity(SubscriptionPayment payment) {
        this.id = payment.id();
        this.tenantId = payment.tenantId();
        this.amount = payment.amount();
        this.currency = payment.currency();
        this.paidOn = payment.paidOn();
        this.method = payment.method().name();
        this.recordedBy = payment.recordedBy();
        this.recordedAt = payment.recordedAt();
    }

    SubscriptionPayment toPayment() {
        return new SubscriptionPayment(id, tenantId, amount, currency, paidOn, PaymentMethod.valueOf(method),
                recordedBy, recordedAt);
    }

    UUID tenantId() {
        return tenantId;
    }

    BigDecimal amount() {
        return amount;
    }

    String currency() {
        return currency;
    }

    LocalDate paidOn() {
        return paidOn;
    }
}
