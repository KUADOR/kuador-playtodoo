package com.kuador.platodo.api.subscriptions.application;

/** Another plan field has this label, without regard to case, or the key derived from it ({@code 409 FIELD_NAME_TAKEN}). */
public final class FieldNameTakenException extends RuntimeException {

    public FieldNameTakenException() {
        super("Plan field name taken");
    }
}
