package com.kuador.platodo.api.audit.infrastructure.identity;

import java.util.UUID;

import com.kuador.platodo.api.audit.application.NotPlatformOwnerException;
import com.kuador.platodo.api.audit.application.PlatformUserNotAuthenticatedException;
import com.kuador.platodo.api.audit.application.port.out.CurrentPlatformOwner;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;

/**
 * The Kuador owner (role {@code SUPER_ADMIN}, ADR-017) of the request, from the access token Identity and Access
 * accepted.
 */
public final class PrincipalCurrentPlatformOwner implements CurrentPlatformOwner {

    @Override
    public UUID require() {
        AuthenticatedPrincipal principal =
                CurrentPrincipal.current().orElseThrow(PlatformUserNotAuthenticatedException::new);
        if (!principal.isSuperAdmin()) {
            throw new NotPlatformOwnerException();
        }
        return principal.accountId();
    }
}
