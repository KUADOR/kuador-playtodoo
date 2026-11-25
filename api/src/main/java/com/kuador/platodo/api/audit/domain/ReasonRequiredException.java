package com.kuador.platodo.api.audit.domain;

/** An access session was requested without a reason. */
public class ReasonRequiredException extends RuntimeException {

    public ReasonRequiredException() {
        super("An access session needs a non-blank reason", null, false, false);
    }
}
