package com.kuador.platodo.api.identity.application.port.out;

import java.time.Duration;
import java.util.Optional;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;

/** Issues and reads the signed access tokens of the api. */
public interface AccessTokens {

    IssuedToken issue(AuthenticatedPrincipal principal);

    /** The principal of a token signed by the api and not expired; empty for any other token. */
    Optional<AuthenticatedPrincipal> read(String token);

    record IssuedToken(String value, Duration timeToLive) {
    }
}
