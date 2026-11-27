package com.kuador.platodo.api.tenancy.domain;

/** Only a suspended or archived complex has a retention period to change (ADR-014). */
public class NotInRetentionException extends RuntimeException {

    public NotInRetentionException() {
        super("The tenant is neither suspended nor archived", null, false, false);
    }
}
