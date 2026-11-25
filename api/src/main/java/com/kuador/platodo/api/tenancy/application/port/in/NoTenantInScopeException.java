package com.kuador.platodo.api.tenancy.application.port.in;

/** Tenant data was accessed without an open {@link TenantScope}; the access is refused (fail closed). */
public class NoTenantInScopeException extends IllegalStateException {

    public NoTenantInScopeException() {
        super("No tenant scope is open: tenant data access is refused");
    }
}
