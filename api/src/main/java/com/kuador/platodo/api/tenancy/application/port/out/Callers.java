package com.kuador.platodo.api.tenancy.application.port.out;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Who calls a Tenancy operation, from the access token Identity and Access accepted for the request.
 * Both checks throw {@link com.kuador.platodo.api.tenancy.application.CallerNotAuthenticatedException} without an
 * account and {@link com.kuador.platodo.api.tenancy.application.CallerNotAllowedException} for any other account.
 */
public interface Callers {

    /** The caller is a Kuador platform user with role {@code PLATFORM_OWNER}. */
    void requirePlatformOwner();

    /** The caller is the owner (staff role {@code OWNER}) of the complex in {@code TenantScope}; returns it. */
    TenantId requireComplexOwner();
}
