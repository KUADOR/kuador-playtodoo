package com.kuador.platodo.api.identity.support;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens;

/** Random token values mapped to the principal they were issued for. */
public final class OpaqueTokens implements AccessTokens {

    public static final Duration TIME_TO_LIVE = Duration.ofHours(1);

    private final Map<String, AuthenticatedPrincipal> issued = new ConcurrentHashMap<>();

    @Override
    public IssuedToken issue(AuthenticatedPrincipal principal) {
        String value = UUID.randomUUID().toString();
        issued.put(value, principal);
        return new IssuedToken(value, TIME_TO_LIVE);
    }

    @Override
    public Optional<AuthenticatedPrincipal> read(String token) {
        return Optional.ofNullable(issued.get(token));
    }
}
