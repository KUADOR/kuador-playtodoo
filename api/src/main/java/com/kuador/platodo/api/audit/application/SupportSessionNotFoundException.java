package com.kuador.platodo.api.audit.application;

/** No support session with this id opened by the caller. */
public class SupportSessionNotFoundException extends RuntimeException {

    public SupportSessionNotFoundException() {
        super("No support session with this id for the caller", null, false, false);
    }
}
