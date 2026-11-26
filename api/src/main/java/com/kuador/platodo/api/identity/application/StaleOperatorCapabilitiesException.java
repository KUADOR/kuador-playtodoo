package com.kuador.platodo.api.identity.application;

/** The version sent with a change no longer matches the operator capabilities ({@code 409 STALE_STATE}). */
public final class StaleOperatorCapabilitiesException extends RuntimeException {

    public StaleOperatorCapabilitiesException() {
        super("The operator capabilities changed since they were read");
    }
}
