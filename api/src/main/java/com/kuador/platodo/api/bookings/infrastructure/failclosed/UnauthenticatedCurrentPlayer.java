package com.kuador.platodo.api.bookings.infrastructure.failclosed;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.PlayerNotAuthenticatedException;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;

/** Used until Identity and Access authenticates players: every request is rejected as unauthenticated. */
public class UnauthenticatedCurrentPlayer implements CurrentPlayer {

    @Override
    public UUID require() {
        throw new PlayerNotAuthenticatedException();
    }
}
