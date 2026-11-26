package com.kuador.platodo.api.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.identity.application.RequestAuthentication.Authenticated;
import com.kuador.platodo.api.identity.application.RequestAuthentication.Reason;
import com.kuador.platodo.api.identity.application.RequestAuthentication.Rejected;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.identity.support.InMemoryAccounts;
import com.kuador.platodo.api.identity.support.OpaqueTokens;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

/** Token acceptance rules per host (PLATODO-26 design, module Identity and access). */
class AuthenticateRequestTest {

    private static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final TenantId B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");

    private final OpaqueTokens tokens = new OpaqueTokens();
    private final InMemoryAccounts staff = new InMemoryAccounts();
    private final InMemoryAccounts players = new InMemoryAccounts();
    private final InMemoryAccounts platformUsers = new InMemoryAccounts();
    private final AuthenticateRequest authenticateRequest =
            new AuthenticateRequest(tokens, staff, players, platformUsers);

    @Test
    void staffTokenIsAcceptedOnTheHostOfItsComplex() {
        AuthenticatedPrincipal staffOfA = AuthenticatedPrincipal.staff(staff.add("a@example.com", "x", "STAFF"), A,
                "STAFF");
        String token = tokens.issue(staffOfA).value();

        assertThat(onHostOf(A, token)).isEqualTo(new Authenticated(staffOfA));
    }

    @Test
    void staffTokenOfAIsRejectedOnTheHostOfB() {
        String token = tokens.issue(
                AuthenticatedPrincipal.staff(staff.add("a@example.com", "x", "STAFF"), A, "STAFF")).value();

        assertThat(onHostOf(B, token)).isEqualTo(new Rejected(Reason.UNAUTHENTICATED));
    }

    @Test
    void staffTokenIsRejectedOnTheGlobalHost() {
        String token = tokens.issue(
                AuthenticatedPrincipal.staff(staff.add("a@example.com", "x", "STAFF"), A, "STAFF")).value();

        assertThat(authenticateRequest.authenticate(token)).isEqualTo(new Rejected(Reason.UNAUTHENTICATED));
    }

    @Test
    void staffTokenOfADisabledAccountIsRejected() {
        UUID staffId = staff.add("a@example.com", "x", "STAFF");
        String token = tokens.issue(AuthenticatedPrincipal.staff(staffId, A, "STAFF")).value();
        staff.disable(staffId);

        assertThat(onHostOf(A, token)).isEqualTo(new Rejected(Reason.UNAUTHENTICATED));
    }

    @Test
    void playerTokenIsAcceptedOnEveryHost() {
        AuthenticatedPrincipal player = AuthenticatedPrincipal.global(
                players.add("p@example.com", "x", "PLAYER"), AccountType.PLAYER, "PLAYER");
        String token = tokens.issue(player).value();

        assertThat(onHostOf(A, token)).isEqualTo(new Authenticated(player));
        assertThat(onHostOf(B, token)).isEqualTo(new Authenticated(player));
        assertThat(authenticateRequest.authenticate(token)).isEqualTo(new Authenticated(player));
    }

    @Test
    void playerTokenOfADisabledAccountIsRejected() {
        UUID playerId = players.add("p@example.com", "x", "PLAYER");
        String token = tokens.issue(AuthenticatedPrincipal.global(playerId, AccountType.PLAYER, "PLAYER")).value();
        players.disable(playerId);

        assertThat(onHostOf(A, token)).isEqualTo(new Rejected(Reason.UNAUTHENTICATED));
    }

    /** On the host of a complex the access session is checked afterwards by Tenancy's PlatformAccessGuard. */
    @Test
    void platformTokenIsAcceptedOnEveryHostWhileTheAccountIsActive() {
        AuthenticatedPrincipal owner = AuthenticatedPrincipal.global(
                platformUsers.add("o@example.com", "x", "SUPER_ADMIN"), AccountType.PLATFORM, "SUPER_ADMIN");
        String token = tokens.issue(owner).value();

        assertThat(authenticateRequest.authenticate(token)).isEqualTo(new Authenticated(owner));
        assertThat(onHostOf(A, token)).isEqualTo(new Authenticated(owner));
    }

    @Test
    void platformTokenOfADisabledAccountIsRejectedOnTheHostOfAComplex() {
        UUID ownerId = platformUsers.add("d@example.com", "x", "SUPER_ADMIN");
        String token = tokens.issue(AuthenticatedPrincipal.global(ownerId, AccountType.PLATFORM, "SUPER_ADMIN"))
                .value();
        platformUsers.disable(ownerId);

        assertThat(onHostOf(A, token)).isEqualTo(new Rejected(Reason.UNAUTHENTICATED));
    }

    @Test
    void unknownTokenIsRejected() {
        assertThat(onHostOf(A, "not-a-token")).isEqualTo(new Rejected(Reason.UNAUTHENTICATED));
        assertThat(authenticateRequest.authenticate("not-a-token")).isEqualTo(new Rejected(Reason.UNAUTHENTICATED));
    }

    private RequestAuthentication onHostOf(TenantId tenant, String token) {
        return TenantScope.call(tenant, () -> authenticateRequest.authenticate(token));
    }
}
