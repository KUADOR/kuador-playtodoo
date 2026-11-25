package com.kuador.platodo.api.bookings.infrastructure.web;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.CreateStaffBooking;
import com.kuador.platodo.api.bookings.domain.BookingHolder;

/**
 * {@code CreateStaffBookingRequest} schema of the API contract. A missing, blank or too long field is a
 * malformed request. Lengths match the columns of the booking table.
 */
record CreateStaffBookingRequest(UUID courtId, Instant start, String holderName, String holderPhone) {

    static final int HOLDER_NAME_MAX_LENGTH = 120;
    static final int HOLDER_PHONE_MAX_LENGTH = 32;

    CreateStaffBookingRequest {
        Objects.requireNonNull(courtId, "courtId");
        Objects.requireNonNull(start, "start");
        holderName = RequestText.required(holderName, "holderName", HOLDER_NAME_MAX_LENGTH);
        holderPhone = RequestText.optional(holderPhone, "holderPhone", HOLDER_PHONE_MAX_LENGTH);
    }

    CreateStaffBooking.Command toCommand() {
        return new CreateStaffBooking.Command(courtId, start, new BookingHolder(holderName, holderPhone));
    }
}
