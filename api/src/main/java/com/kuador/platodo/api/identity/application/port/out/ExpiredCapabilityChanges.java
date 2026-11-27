package com.kuador.platodo.api.identity.application.port.out;

import java.time.Instant;

/** Deletion of the capability changes past their retention (table {@code capability_change}, ADR-016). */
@FunctionalInterface
public interface ExpiredCapabilityChanges {

    /**
     * Deletes every change made before {@code cutoff}. Needs a transaction of the global schema.
     *
     * @return how many changes were deleted
     */
    int deleteChangedBefore(Instant cutoff);
}
