package com.kuador.platodo.api.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

/** ADR-016: the audit is deleted once it is older than its retention, counted back from now. */
class DeleteExpiredAccessAuditTest {

    private static final Instant NOW = Instant.parse("2026-12-01T03:30:00Z");
    private static final Duration ONE_YEAR = Duration.ofDays(365);

    private final List<Instant> cutoffs = new ArrayList<>();

    @Test
    void theCutoffIsTheRetentionBeforeNow() {
        DeleteExpiredAccessAudit delete = new DeleteExpiredAccessAudit(cutoff -> {
            cutoffs.add(cutoff);
            return 3;
        }, TransactionOperations.withoutTransaction(), Clock.fixed(NOW, ZoneOffset.UTC), ONE_YEAR);

        assertThat(delete.run()).isEqualTo(3);
        assertThat(cutoffs).containsExactly(NOW.minus(ONE_YEAR));
    }

    @Test
    void theRetentionMustBePositive() {
        assertThatThrownBy(() -> new DeleteExpiredAccessAudit(cutoff -> 0, TransactionOperations.withoutTransaction(),
                Clock.systemUTC(), Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
