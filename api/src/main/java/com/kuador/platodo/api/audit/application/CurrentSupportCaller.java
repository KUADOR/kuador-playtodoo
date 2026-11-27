package com.kuador.platodo.api.audit.application;

import com.kuador.platodo.api.audit.domain.SupportCaller;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerNotAuthenticatedException;
import com.kuador.platodo.api.identity.domain.Capability;

/**
 * The Kuador team member of the current request, with the capabilities enabled right now (ADR-017): read on every
 * call, so a {@code TENANT_DATA_ACCESS} the SUPER_ADMIN disables stops the operator's next request.
 */
public final class CurrentSupportCaller {

    private final CapabilityGuard guard;

    public CurrentSupportCaller(CapabilityGuard guard) {
        this.guard = guard;
    }

    /**
     * @throws PlatformCallerNotAuthenticatedException if there is no authenticated caller
     * @throws PlatformCallerForbiddenException        if the caller is not a known Kuador role
     */
    public SupportCaller require() {
        PlatformCaller caller = guard.requirePlatformUser();
        boolean tenantDataAccess = guard.effectiveCapabilities(caller).contains(Capability.TENANT_DATA_ACCESS);
        return new SupportCaller(caller.id(), caller.isSuperAdmin(), tenantDataAccess);
    }
}
