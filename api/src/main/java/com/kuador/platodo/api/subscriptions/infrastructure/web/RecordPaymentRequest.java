package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.time.LocalDate;

import com.kuador.platodo.api.subscriptions.domain.PaymentDraft;

/**
 * Schema {@code RecordPaymentRequest}. {@code amount} and {@code method} are read as text so the rules of
 * {@link PaymentDraft} report them as {@code 422 VALIDATION_FAILED}; a {@code paidOn} that is not a date is a
 * malformed body.
 */
record RecordPaymentRequest(String amount, LocalDate paidOn, String method) {

    PaymentDraft toDraft() {
        return new PaymentDraft(amount, paidOn, method);
    }
}
