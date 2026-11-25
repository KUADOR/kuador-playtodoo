package com.kuador.platodo.api.tenancy.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TenancyUseCasesTest {

    /** Linux clocks give nanoseconds; an answered time must equal the one PostgreSQL stores (microseconds). */
    @Test
    void theUseCasesReadTheClockAtTheDatabasePrecision() {
        Clock nanoseconds = Clock.fixed(Instant.parse("2026-11-25T18:26:56.543334375Z"), ZoneOffset.UTC);

        assertThat(TenancyUseCases.atDatabasePrecision(nanoseconds).instant())
                .isEqualTo(Instant.parse("2026-11-25T18:26:56.543334Z"));
    }
}
