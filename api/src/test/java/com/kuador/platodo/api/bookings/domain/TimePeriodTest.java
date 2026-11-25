package com.kuador.platodo.api.bookings.domain;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TimePeriodTest {

    private static final Instant EIGHT = Instant.parse("2026-12-04T20:00:00Z");
    private static final Instant NINE = Instant.parse("2026-12-04T21:00:00Z");

    @Test
    void acceptsAStartBeforeItsEnd() {
        assertThatNoException().isThrownBy(() -> new TimePeriod(EIGHT, NINE));
    }

    @Test
    void rejectsAnEmptyPeriod() {
        assertThatThrownBy(() -> new TimePeriod(EIGHT, EIGHT)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnEndBeforeItsStart() {
        assertThatThrownBy(() -> new TimePeriod(NINE, EIGHT)).isInstanceOf(IllegalArgumentException.class);
    }
}
