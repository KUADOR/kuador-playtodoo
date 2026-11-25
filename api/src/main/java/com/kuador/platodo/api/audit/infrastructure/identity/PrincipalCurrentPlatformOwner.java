package com.kuador.platodo.api.audit.infrastructure.identity;

import java.util.UUID;

import com.kuador.platodo.api.audit.application.NotPlatformOwnerException;
import com.kuador.platodo.api.audit.application.PlatformUserNotAuthenticatedException;
import com.kuador.platodo.api.audit.application.port.out.CurrentPlatformOwner;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;

/** The Kuador owner of the request, from the access token Identity and Access accepted. */
public final class PrincipalCurrentPlatformOwner implements CurrentPlatformOwner {

    @Override
    public UUID require() {
        AuthenticatedPrincipal principal =
                CurrentPrincipal.current().orElseThrow(PlatformUserNotAuthenticatedException::new);
        if (!principal.isPlatformOwner()) {
            throw new NotPlatformOwnerException();
        }
        return principal.accountId();
    }
}
