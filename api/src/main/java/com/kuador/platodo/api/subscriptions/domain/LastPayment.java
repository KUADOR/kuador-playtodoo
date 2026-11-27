package com.kuador.platodo.api.subscriptions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** The latest payment recorded for a complex: the one with the latest {@code paidOn}, then the latest recorded. */
public record LastPayment(BigDecimal amount, String currency, LocalDate paidOn) {

    public LastPayment {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(paidOn, "paidOn");
    }
}
