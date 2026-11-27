package com.kuador.platodo.api.subscriptions.application.port.out;

import java.time.Instant;

/** Deletion of the plan and status changes past their retention (table {@code subscription_history}, ADR-016). */
@FunctionalInterface
public interface ExpiredSubscriptionHistory {

    /**
     * Deletes every entry changed before {@code cutoff}, of every complex. Needs a transaction of the global schema.
     *
     * @return how many entries were deleted
     */
    int deleteChangedBefore(Instant cutoff);
}
