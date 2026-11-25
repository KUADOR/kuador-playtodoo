package com.kuador.platodo.api.bookings.infrastructure.web;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.BlockSlot;

/** {@code BlockSlotRequest} schema of the API contract. A missing or too long field is a malformed request. */
record BlockSlotRequest(UUID courtId, Instant start, String reason) {

    static final int REASON_MAX_LENGTH = 200;

    BlockSlotRequest {
        Objects.requireNonNull(courtId, "courtId");
        Objects.requireNonNull(start, "start");
        reason = RequestText.optional(reason, "reason", REASON_MAX_LENGTH);
    }

    BlockSlot.Command toCommand() {
        return new BlockSlot.Command(courtId, start, reason);
    }
}
