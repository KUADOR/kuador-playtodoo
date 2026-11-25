package com.kuador.platodo.api.bookings.infrastructure.identity;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.PlayerNotAuthenticatedException;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;

/**
 * The player of the request, from the access token Identity and Access accepted. Only the global account id reaches
 * the complex's data: bookings keep {@code player_id} and never copy the player's e-mail or name.
 */
public final class PrincipalCurrentPlayer implements CurrentPlayer {

    @Override
    public UUID require() {
        return CurrentPrincipal.current()
                .filter(principal -> principal.is(AccountType.PLAYER))
                .map(AuthenticatedPrincipal::accountId)
                .orElseThrow(PlayerNotAuthenticatedException::new);
    }
}
