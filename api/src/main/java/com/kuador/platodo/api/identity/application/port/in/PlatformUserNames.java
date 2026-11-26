package com.kuador.platodo.api.identity.application.port.in;

import java.util.Optional;
import java.util.UUID;

/** Visible names of Kuador team members, for modules that show who did something. Global schema only. */
@FunctionalInterface
public interface PlatformUserNames {

    /** The visible name of the platform user {@code id}; empty if there is none. */
    Optional<String> displayNameOf(UUID id);
}
