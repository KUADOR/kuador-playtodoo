package com.kuador.platodo.api.identity.application.port.in;

import java.util.Objects;
import java.util.UUID;

import com.kuador.platodo.api.identity.domain.PlatformRole;

/** The Kuador team member a platform request acts as, once {@link CapabilityGuard} let it through. */
public record PlatformCaller(UUID id, PlatformRole role) {

    public PlatformCaller {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
    }

    public boolean isSuperAdmin() {
        return role == PlatformRole.SUPER_ADMIN;
    }
}
