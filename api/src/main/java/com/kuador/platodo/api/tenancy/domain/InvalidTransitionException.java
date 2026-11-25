package com.kuador.platodo.api.tenancy.domain;

/** The lifecycle state of the complex does not allow the requested operation (ADR-013). */
public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException() {
        super("The tenant is not in a state that allows this operation", null, false, false);
    }
}
