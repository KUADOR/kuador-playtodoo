package com.kuador.platodo.api.identity.infrastructure.web;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.identity.application.CurrentPlatformUser;
import com.kuador.platodo.api.identity.domain.Capability;

/** Schema {@code CurrentPlatformUser}; capabilities in declaration order. */
record CurrentPlatformUserResponse(UUID id, String displayName, String role, List<String> capabilities) {

    static CurrentPlatformUserResponse of(CurrentPlatformUser user) {
        List<String> capabilities = Arrays.stream(Capability.values())
                .filter(user.capabilities()::contains)
                .map(Capability::name)
                .toList();
        return new CurrentPlatformUserResponse(user.id(), user.displayName(), user.role().name(), capabilities);
    }
}
