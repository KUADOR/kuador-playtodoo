package com.kuador.platodo.api.subscriptions.domain;

import java.util.List;

/** Fields of a payment that break their rules ({@code 422 VALIDATION_FAILED}). */
public final class InvalidPaymentException extends RuntimeException {

    private final transient List<FieldError> fieldErrors;

    public InvalidPaymentException(List<FieldError> fieldErrors) {
        super("Invalid fields: " + fieldErrors);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
