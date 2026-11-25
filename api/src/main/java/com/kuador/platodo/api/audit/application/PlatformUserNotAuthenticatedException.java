package com.kuador.platodo.api.audit.application;

/** A platform access operation was called without an authenticated account. */
public class PlatformUserNotAuthenticatedException extends RuntimeException {

    public PlatformUserNotAuthenticatedException() {
        super("An authenticated platform user is required", null, false, false);
    }
}
