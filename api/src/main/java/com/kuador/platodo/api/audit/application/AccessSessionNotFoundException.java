package com.kuador.platodo.api.audit.application;

/** No access session with this id opened by the caller. */
public class AccessSessionNotFoundException extends RuntimeException {

    public AccessSessionNotFoundException() {
        super("No access session with this id for the caller", null, false, false);
    }
}
