package com.kuador.platodo.api.subscriptions.domain;

/** Basic plan fields are never edited nor retired ({@code 422 BASIC_FIELD_READ_ONLY}). */
public final class BasicFieldReadOnlyException extends RuntimeException {

    public BasicFieldReadOnlyException(String key) {
        super("The basic plan field " + key + " is read-only");
    }
}
