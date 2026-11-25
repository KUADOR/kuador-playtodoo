package com.kuador.platodo.api.identity.infrastructure.web;

import com.kuador.platodo.api.identity.application.port.out.AccessTokens.IssuedToken;

/** {@code AccessToken} of the contract. */
record AccessTokenResponse(String accessToken, String tokenType, long expiresIn) {

    private static final String BEARER = "Bearer";

    static AccessTokenResponse of(IssuedToken token) {
        return new AccessTokenResponse(token.value(), BEARER, token.timeToLive().toSeconds());
    }
}
