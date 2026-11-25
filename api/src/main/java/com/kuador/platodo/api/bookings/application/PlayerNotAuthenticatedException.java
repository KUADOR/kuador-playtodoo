package com.kuador.platodo.api.bookings.application;

/** The request has no authenticated player. */
public class PlayerNotAuthenticatedException extends RuntimeException {

    public PlayerNotAuthenticatedException() {
        super("An authenticated player is required");
    }
}
