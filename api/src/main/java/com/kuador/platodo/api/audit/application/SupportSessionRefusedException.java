package com.kuador.platodo.api.audit.application;

import com.kuador.platodo.api.audit.domain.DenialCode;

/** A support session was not opened; the refusal was audited with {@link #code()}. */
public class SupportSessionRefusedException extends RuntimeException {

    private final DenialCode code;

    public SupportSessionRefusedException(DenialCode code) {
        super("Support session refused: " + code, null, false, false);
        this.code = code;
    }

    public DenialCode code() {
        return code;
    }
}
