package com.kuador.platodo.api.audit.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.application.port.out.ExpiredAccessAudit;

/**
 * Deletes the audit of the Kuador accesses older than its retention (ADR-016: one year by decision of the Kuador
 * team), in one transaction of the global schema. Run by a scheduled job; running it twice deletes nothing more.
 */
public final class DeleteExpiredAccessAudit {

    private final ExpiredAccessAudit expired;
    private final TransactionOperations transaction;
    private final Clock clock;
    private final Duration retention;

    public DeleteExpiredAccessAudit(ExpiredAccessAudit expired, TransactionOperations transaction, Clock clock,
            Duration retention) {
        this.expired = expired;
        this.transaction = transaction;
        this.clock = clock;
        this.retention = requirePositive(retention);
    }

    /** @return how many rows were deleted */
    public int run() {
        return Objects.requireNonNull(transaction.execute(
                status -> expired.deleteBefore(clock.instant().minus(retention))));
    }

    private static Duration requirePositive(Duration retention) {
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("The retention of the access audit must be positive");
        }
        return retention;
    }
}
