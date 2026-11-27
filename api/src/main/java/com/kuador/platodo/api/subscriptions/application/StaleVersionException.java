package com.kuador.platodo.api.subscriptions.application;

/** The version sent with a change no longer matches the plan, plan field or complex ({@code 409 STALE_STATE}). */
public final class StaleVersionException extends RuntimeException {

    public StaleVersionException() {
        super("The resource changed since it was read");
    }
}
