package com.kuador.platodo.api.subscriptions.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.subscriptions.application.port.out.ExpiredSubscriptionHistory;

/**
 * Deletes the subscription history older than its retention (ADR-016: one year by decision of the Kuador team), in
 * one transaction of the global schema. Subscriptions and payments are not touched. Run by a scheduled job; running it
 * twice deletes nothing more.
 */
public final class DeleteExpiredSubscriptionHistory {

    private final ExpiredSubscriptionHistory expired;
    private final TransactionOperations transaction;
    private final Clock clock;
    private final Duration retention;

    public DeleteExpiredSubscriptionHistory(ExpiredSubscriptionHistory expired, TransactionOperations transaction,
            Clock clock, Duration retention) {
        this.expired = expired;
        this.transaction = transaction;
        this.clock = clock;
        this.retention = requirePositive(retention);
    }

    /** @return how many entries were deleted */
    public int run() {
        return Objects.requireNonNull(transaction.execute(
                status -> expired.deleteChangedBefore(clock.instant().minus(retention))));
    }

    private static Duration requirePositive(Duration retention) {
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("The retention of the subscription history must be positive");
        }
        return retention;
    }
}
