package com.kuador.platodo.api.subscriptions.application;

/** No complex with a subscription has the requested id ({@code 404 COMPLEX_NOT_FOUND}). */
public final class ComplexNotFoundException extends RuntimeException {

    public ComplexNotFoundException() {
        super("No complex with this id", null, false, false);
    }
}
