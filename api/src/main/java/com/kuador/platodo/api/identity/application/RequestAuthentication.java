package com.kuador.platodo.api.identity.application;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;

/** Outcome of checking the access token of a request against the host it was sent to. */
public sealed interface RequestAuthentication {

    record Authenticated(AuthenticatedPrincipal principal) implements RequestAuthentication {
    }

    record Rejected(Reason reason) implements RequestAuthentication {
    }

    enum Reason {
        /** Invalid or expired token, inactive account, or a staff token outside its own complex. */
        UNAUTHENTICATED,
        /** A Kuador token on the host of a complex without a valid access session. */
        ACCESS_SESSION_INVALID
    }
}
