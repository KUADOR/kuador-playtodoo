package com.kuador.platodo.api.identity.application;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.PlatformRole;

/** The authenticated Kuador team member and the capabilities effective for this request. */
public record CurrentPlatformUser(UUID id, String displayName, PlatformRole role, Set<Capability> capabilities) {

    public CurrentPlatformUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(role, "role");
        capabilities = Set.copyOf(capabilities);
    }
}
