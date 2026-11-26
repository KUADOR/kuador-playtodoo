package com.kuador.platodo.api.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Visible data of Kuador team members, in the global schema. */
@FunctionalInterface
public interface PlatformUserProfiles {

    /** The visible name of the platform user {@code id}; empty if there is none. */
    Optional<String> displayNameOf(UUID id);
}
