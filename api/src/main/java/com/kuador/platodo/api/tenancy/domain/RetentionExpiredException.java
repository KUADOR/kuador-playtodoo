package com.kuador.platodo.api.tenancy.domain;

/** The purge date of a suspended complex has been reached: it can no longer be reactivated. */
public class RetentionExpiredException extends RuntimeException {

    public RetentionExpiredException() {
        super("The purge date has passed", null, false, false);
    }
}
