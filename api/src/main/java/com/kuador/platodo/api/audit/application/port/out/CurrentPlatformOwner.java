package com.kuador.platodo.api.audit.application.port.out;

import java.util.UUID;

import com.kuador.platodo.api.audit.application.NotPlatformOwnerException;
import com.kuador.platodo.api.audit.application.PlatformUserNotAuthenticatedException;

/** The Kuador owner who calls a platform access operation. */
public interface CurrentPlatformOwner {

    /**
     * The platform user id of the caller.
     *
     * @throws PlatformUserNotAuthenticatedException if the request has no authenticated account
     * @throws NotPlatformOwnerException             if the account is not a Kuador platform owner
     */
    UUID require();
}
