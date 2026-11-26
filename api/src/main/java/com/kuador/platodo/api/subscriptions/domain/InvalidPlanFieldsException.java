package com.kuador.platodo.api.subscriptions.domain;

import java.util.List;

/** Fields of a plan or of a plan field definition that break their rules ({@code 422 VALIDATION_FAILED}). */
public final class InvalidPlanFieldsException extends RuntimeException {

    private final transient List<FieldError> fieldErrors;

    public InvalidPlanFieldsException(List<FieldError> fieldErrors) {
        super("Invalid fields: " + fieldErrors);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
