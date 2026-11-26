package com.kuador.platodo.api.identity.application;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerNotAuthenticatedException;
import com.kuador.platodo.api.identity.application.port.out.PlatformUserProfiles;

/** {@code GET /me}: who the caller is and what they may do now, so the backoffice can hide what they cannot. */
public final class GetCurrentPlatformUser {

    private final CapabilityGuard guard;
    private final PlatformUserProfiles profiles;

    public GetCurrentPlatformUser(CapabilityGuard guard, PlatformUserProfiles profiles) {
        this.guard = guard;
        this.profiles = profiles;
    }

    public CurrentPlatformUser get() {
        PlatformCaller caller = guard.requirePlatformUser();
        String displayName = profiles.displayNameOf(caller.id())
                .orElseThrow(PlatformCallerNotAuthenticatedException::new);
        return new CurrentPlatformUser(caller.id(), displayName, caller.role(), guard.effectiveCapabilities(caller));
    }
}
