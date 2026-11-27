package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.List;

import com.kuador.platodo.api.subscriptions.domain.FieldError;

/** A retention period out of the limits of the contract ({@code 422 VALIDATION_FAILED}). */
final class InvalidRetentionRequestException extends RuntimeException {

    private final transient List<FieldError> fieldErrors;

    InvalidRetentionRequestException(List<FieldError> fieldErrors) {
        super("Invalid fields: " + fieldErrors, null, false, false);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
