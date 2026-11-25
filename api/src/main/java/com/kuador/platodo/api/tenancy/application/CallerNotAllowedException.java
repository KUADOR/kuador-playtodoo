package com.kuador.platodo.api.tenancy.application;

/** The account of the request is valid but its type or role does not allow the operation. */
public class CallerNotAllowedException extends RuntimeException {

    public CallerNotAllowedException() {
        super("The account is not allowed to perform this operation", null, false, false);
    }
}
