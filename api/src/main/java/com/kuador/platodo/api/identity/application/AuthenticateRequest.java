package com.kuador.platodo.api.identity.application;

import java.util.Optional;

import com.kuador.platodo.api.identity.application.RequestAuthentication.Authenticated;
import com.kuador.platodo.api.identity.application.RequestAuthentication.Reason;
import com.kuador.platodo.api.identity.application.RequestAuthentication.Rejected;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens;
import com.kuador.platodo.api.identity.application.port.out.AccountDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

/**
 * Token acceptance rules of the PLATODO-26 design. The host context is the {@link TenantScope}: open on the host of a
 * complex, absent on the global host.
 * <ul>
 *   <li>{@code STAFF}: only on the host of its own complex ({@code tid} = resolved tenant) and while the account is
 *       active there. Elsewhere, the global host included, it is not accepted.</li>
 *   <li>{@code PLAYER}: on every host, while the global account is active.</li>
 *   <li>{@code PLATFORM}: on the global host while the account is active. On the host of a complex it needs an
 *       access session, which this version does not grant yet (ADR-011), so it is refused.</li>
 * </ul>
 */
public final class AuthenticateRequest {

    private static final RequestAuthentication UNAUTHENTICATED = new Rejected(Reason.UNAUTHENTICATED);
    private static final RequestAuthentication ACCESS_SESSION_INVALID = new Rejected(Reason.ACCESS_SESSION_INVALID);

    private final AccessTokens tokens;
    private final AccountDirectory staff;
    private final AccountDirectory players;
    private final AccountDirectory platformUsers;

    public AuthenticateRequest(AccessTokens tokens, AccountDirectory staff, AccountDirectory players,
            AccountDirectory platformUsers) {
        this.tokens = tokens;
        this.staff = staff;
        this.players = players;
        this.platformUsers = platformUsers;
    }

    public RequestAuthentication authenticate(String token) {
        Optional<TenantId> hostTenant = TenantScope.current();
        return tokens.read(token)
                .map(principal -> accept(principal, hostTenant))
                .orElse(UNAUTHENTICATED);
    }

    private RequestAuthentication accept(AuthenticatedPrincipal principal, Optional<TenantId> hostTenant) {
        return switch (principal.type()) {
            case STAFF -> hostTenant.equals(principal.staffTenant())
                    ? activeIn(staff, principal)
                    : UNAUTHENTICATED;
            case PLAYER -> activeIn(players, principal);
            case PLATFORM -> hostTenant.isPresent()
                    ? ACCESS_SESSION_INVALID
                    : activeIn(platformUsers, principal);
        };
    }

    private static RequestAuthentication activeIn(AccountDirectory accounts, AuthenticatedPrincipal principal) {
        return accounts.isActive(principal.accountId()) ? new Authenticated(principal) : UNAUTHENTICATED;
    }
}
