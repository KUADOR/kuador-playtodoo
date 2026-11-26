package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.subscription_payment} (migration V8), read-only here: payments are recorded by PLATODO-85. */
@Entity
@Table(name = "subscription_payment")
class SubscriptionPaymentEntity {

    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;
    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected SubscriptionPaymentEntity() {
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
