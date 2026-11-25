package com.kuador.platodo.api.audit.domain;

/** The reason of an access session is longer than {@link AccessReason#MAX_LENGTH}. */
public class ReasonTooLongException extends RuntimeException {

    public ReasonTooLongException() {
        super("The reason is longer than " + AccessReason.MAX_LENGTH + " characters", null, false, false);
    }
}
