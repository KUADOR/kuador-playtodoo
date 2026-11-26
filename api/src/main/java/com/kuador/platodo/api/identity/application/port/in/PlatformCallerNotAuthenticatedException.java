package com.kuador.platodo.api.identity.application.port.in;

/** A platform operation was called without an accepted access token ({@code 401 UNAUTHENTICATED}). */
public final class PlatformCallerNotAuthenticatedException extends RuntimeException {

    public PlatformCallerNotAuthenticatedException() {
        super("A platform access token is required");
    }
}
