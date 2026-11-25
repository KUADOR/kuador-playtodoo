package com.kuador.platodo.api.bookings.application.port.out;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.PlayerNotAuthenticatedException;

/** The authenticated player of the current request, provided by Identity and Access. */
public interface CurrentPlayer {

    /**
     * @throws PlayerNotAuthenticatedException if the request has no authenticated player
     */
    UUID require();
}
