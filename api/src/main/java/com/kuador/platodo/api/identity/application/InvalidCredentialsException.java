package com.kuador.platodo.api.identity.application;

/** Unknown account, disabled account or wrong password: the caller is never told which. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials", null, false, false);
    }
}
