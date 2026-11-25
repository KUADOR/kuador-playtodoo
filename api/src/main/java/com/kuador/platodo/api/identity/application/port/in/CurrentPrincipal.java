package com.kuador.platodo.api.identity.application.port.in;

import java.util.Objects;
import java.util.Optional;

/**
 * The authenticated account of the current request, set by Identity and Access once the access token is accepted
 * for the host of the request. Like {@code TenantScope} it is a {@link ScopedValue}: it ends with the request and is
 * not inherited by other threads. Requests without a token have no principal.
 */
public final class CurrentPrincipal {

    private static final ScopedValue<AuthenticatedPrincipal> CURRENT = ScopedValue.newInstance();

    private CurrentPrincipal() {
    }

    /** Calls {@code operation} acting as {@code principal} and returns its result. */
    public static <T, X extends Throwable> T call(AuthenticatedPrincipal principal,
            ScopedValue.CallableOp<? extends T, X> operation) throws X {
        return ScopedValue.where(CURRENT, Objects.requireNonNull(principal, "principal")).call(operation);
    }

    /** The authenticated account of the current request, if any. */
    public static Optional<AuthenticatedPrincipal> current() {
        return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
    }
}
