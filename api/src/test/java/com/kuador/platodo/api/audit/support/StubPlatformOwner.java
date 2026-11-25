package com.kuador.platodo.api.audit.support;

import java.util.UUID;
import java.util.function.Supplier;

import com.kuador.platodo.api.audit.application.NotPlatformOwnerException;
import com.kuador.platodo.api.audit.application.PlatformUserNotAuthenticatedException;
import com.kuador.platodo.api.audit.application.port.out.CurrentPlatformOwner;

/** The caller of a platform access use case, set by each test. */
public final class StubPlatformOwner implements CurrentPlatformOwner {

    private Supplier<UUID> caller;

    public StubPlatformOwner(UUID ownerId) {
        actAs(ownerId);
    }

    public void actAs(UUID ownerId) {
        caller = () -> ownerId;
    }

    public void actAsNonOwner() {
        caller = () -> {
            throw new NotPlatformOwnerException();
        };
    }

    public void actAnonymously() {
        caller = () -> {
            throw new PlatformUserNotAuthenticatedException();
        };
    }

    @Override
    public UUID require() {
        return caller.get();
    }
}
