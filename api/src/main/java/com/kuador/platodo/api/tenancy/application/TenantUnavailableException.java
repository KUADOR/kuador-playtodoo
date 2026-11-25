package com.kuador.platodo.api.tenancy.application;

/** The complex stopped being active while the request was running; it is treated as "not available". */
public class TenantUnavailableException extends RuntimeException {

    public TenantUnavailableException() {
        super("The complex is not available", null, false, false);
    }
}
