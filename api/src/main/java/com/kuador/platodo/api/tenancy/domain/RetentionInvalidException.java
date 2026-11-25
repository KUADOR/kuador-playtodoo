package com.kuador.platodo.api.tenancy.domain;

/** A new retention would put the purge date in the past, which would purge the complex as a side effect. */
public class RetentionInvalidException extends RuntimeException {

    public RetentionInvalidException() {
        super("The resulting purge date must be in the future", null, false, false);
    }
}
