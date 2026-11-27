package com.kuador.platodo.api.identity.application.port.in;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Visible names of Kuador team members, for modules that show who did something. Global schema only. */
public interface PlatformUserNames {

    /** The visible name of the platform user {@code id}; empty if there is none. */
    Optional<String> displayNameOf(UUID id);

    /** The visible names of the platform users among {@code ids} that exist, read in one query (no N+1). */
    Map<UUID, String> displayNamesOf(Collection<UUID> ids);
}
