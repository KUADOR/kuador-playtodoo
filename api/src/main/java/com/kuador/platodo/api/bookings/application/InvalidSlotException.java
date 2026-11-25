package com.kuador.platodo.api.bookings.application;

import java.time.Instant;
import java.util.UUID;

/** The requested start is in the past or is not the start of a valid slot of the court. */
public class InvalidSlotException extends RuntimeException {

    public InvalidSlotException(UUID courtId, Instant start) {
        super("Start " + start + " is not a valid future slot of court " + courtId);
    }
}
