package com.kuador.platodo.api.bookings.infrastructure.web;

import java.time.Instant;

import com.kuador.platodo.api.bookings.domain.TimePeriod;

/** {@code OccupiedRange} schema of the API contract. */
record OccupiedRangeResponse(Instant start, Instant end) {

    static OccupiedRangeResponse of(TimePeriod period) {
        return new OccupiedRangeResponse(period.start(), period.end());
    }
}
