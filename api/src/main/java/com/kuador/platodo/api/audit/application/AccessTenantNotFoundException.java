package com.kuador.platodo.api.audit.application;

/** No complex with the requested id, or none that can still be reached on a host. */
public class AccessTenantNotFoundException extends RuntimeException {

    public AccessTenantNotFoundException() {
        super("No tenant with this id", null, false, false);
    }
}
