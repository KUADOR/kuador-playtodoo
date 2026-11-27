package com.kuador.platodo.api.audit.application;

/** No complex with the requested id. */
public class SupportComplexNotFoundException extends RuntimeException {

    public SupportComplexNotFoundException() {
        super("No complex with this id", null, false, false);
    }
}
