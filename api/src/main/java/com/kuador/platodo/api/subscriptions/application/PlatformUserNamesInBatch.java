package com.kuador.platodo.api.subscriptions.application;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;

/** Resolves the visible names of the platform users of one page in a single query. */
final class PlatformUserNamesInBatch {

    private PlatformUserNamesInBatch() {
    }

    /**
     * A lookup of the names of {@code ids}. Asking it for any other id, or for a user that does not exist, is a broken
     * reference of the global schema and fails.
     */
    static Function<UUID, String> of(PlatformUserNames names, Collection<UUID> ids) {
        Map<UUID, String> found = names.displayNamesOf(ids);
        return id -> {
            String name = found.get(id);
            if (name == null) {
                throw new IllegalStateException("Unknown platform user " + id);
            }
            return name;
        };
    }
}
