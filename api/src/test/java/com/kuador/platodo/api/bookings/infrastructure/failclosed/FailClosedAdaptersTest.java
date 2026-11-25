package com.kuador.platodo.api.bookings.infrastructure.failclosed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FailClosedAdaptersTest {

    @Test
    void withoutCourtConfigurationNoCourtExistsAndNoStartIsValid() {
        UnconfiguredCourtSlotRules rules = new UnconfiguredCourtSlotRules();
        UUID courtId = UUID.randomUUID();

        assertThat(rules.slotDurationOf(courtId)).isEmpty();
        assertThat(rules.isValidSlotStart(courtId, Instant.parse("2030-01-01T20:00:00Z"))).isFalse();
    }
}
