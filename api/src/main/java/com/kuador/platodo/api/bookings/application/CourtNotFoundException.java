package com.kuador.platodo.api.bookings.application;

import java.util.UUID;

/** The court does not exist in the active tenant. */
public class CourtNotFoundException extends RuntimeException {

    public CourtNotFoundException(UUID courtId) {
        super("Court " + courtId + " does not exist in the active tenant");
    }
}
