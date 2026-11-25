package com.kuador.platodo.api.bookings.application;

/** The request has no authenticated staff member. */
public class StaffNotAuthenticatedException extends RuntimeException {

    public StaffNotAuthenticatedException() {
        super("An authenticated staff member is required");
    }
}
