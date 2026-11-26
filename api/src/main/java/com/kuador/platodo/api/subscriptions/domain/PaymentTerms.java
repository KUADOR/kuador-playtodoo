package com.kuador.platodo.api.subscriptions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** The checked amount, date and method of a payment; see {@link PaymentDraft#check(LocalDate)}. */
public record PaymentTerms(BigDecimal amount, LocalDate paidOn, PaymentMethod method) {

    public PaymentTerms {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(paidOn, "paidOn");
        Objects.requireNonNull(method, "method");
    }
}
