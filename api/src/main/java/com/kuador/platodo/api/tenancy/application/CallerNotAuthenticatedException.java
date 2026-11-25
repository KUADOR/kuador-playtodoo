package com.kuador.platodo.api.tenancy.application;

/** The operation needs an account and the request has none. */
public class CallerNotAuthenticatedException extends RuntimeException {

    public CallerNotAuthenticatedException() {
        super("An authenticated account is required", null, false, false);
    }
}
