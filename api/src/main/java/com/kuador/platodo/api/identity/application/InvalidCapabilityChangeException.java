package com.kuador.platodo.api.identity.application;

/** A change request without changes, or naming one capability twice ({@code 400 INVALID_REQUEST}). */
public final class InvalidCapabilityChangeException extends RuntimeException {

    public InvalidCapabilityChangeException(String message) {
        super(message);
    }
}
