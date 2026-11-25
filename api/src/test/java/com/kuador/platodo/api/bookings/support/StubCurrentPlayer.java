package com.kuador.platodo.api.bookings.support;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.PlayerNotAuthenticatedException;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;

/** The player the current thread acts as; concurrent tests give each thread its own player. */
public final class StubCurrentPlayer implements CurrentPlayer {

    private final ThreadLocal<UUID> player = new ThreadLocal<>();

    public void actAs(UUID playerId) {
        player.set(playerId);
    }

    @Override
    public UUID require() {
        UUID playerId = player.get();
        if (playerId == null) {
            throw new PlayerNotAuthenticatedException();
        }
        return playerId;
    }
}
