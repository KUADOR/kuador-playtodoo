package com.kuador.platodo.api.bookings.application;

import java.util.UUID;

/**
 * A block was requested on a slot that an active booking occupies. A block never cancels a booking:
 * the staff must cancel that booking first (decided by the team, criterion 9).
 */
public class SlotHasBookingException extends RuntimeException {

    private final UUID conflictingBookingId;

    public SlotHasBookingException(UUID conflictingBookingId) {
        super("Booking " + conflictingBookingId + " occupies the slot; cancel it before blocking");
        this.conflictingBookingId = conflictingBookingId;
    }

    public UUID conflictingBookingId() {
        return conflictingBookingId;
    }
}
