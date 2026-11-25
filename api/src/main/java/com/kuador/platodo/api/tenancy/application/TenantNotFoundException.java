package com.kuador.platodo.api.tenancy.application;

/** No complex has the requested id. */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException() {
        super("No tenant with this id", null, false, false);
    }
}
