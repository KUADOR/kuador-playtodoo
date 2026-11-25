package com.kuador.platodo.api.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens.IssuedToken;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.identity.support.InMemoryAccounts;
import com.kuador.platodo.api.identity.support.OpaqueTokens;
import com.kuador.platodo.api.identity.support.PlainPasswords;
import com.kuador.platodo.api.tenancy.application.port.in.NoTenantInScopeException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

class LogInTest {

    private static final TenantId TENANT = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final String PASSWORD = "correct horse";

    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final PlainPasswords passwords = new PlainPasswords();
    private final OpaqueTokens tokens = new OpaqueTokens();

    @Test
    void staffTokenIsBoundToTheTenantOfTheHost() {
        UUID staffId = accounts.add("admin@example.com", PASSWORD, "ADMIN");
        LogIn logIn = new LogIn(AccountType.STAFF, accounts, passwords, tokens);

        IssuedToken token = TenantScope.call(TENANT, () -> logIn.logIn("admin@example.com", PASSWORD));

        assertThat(tokens.read(token.value()))
                .contains(AuthenticatedPrincipal.staff(staffId, TENANT, "ADMIN"));
    }

    @Test
    void staffLoginOutsideATenantFailsClosed() {
        accounts.add("admin@example.com", PASSWORD, "ADMIN");
        LogIn logIn = new LogIn(AccountType.STAFF, accounts, passwords, tokens);

        assertThatThrownBy(() -> logIn.logIn("admin@example.com", PASSWORD))
                .isInstanceOf(NoTenantInScopeException.class);
    }

    @Test
    void playerTokenHasNoTenant() {
        UUID playerId = accounts.add("player@example.com", PASSWORD, "PLAYER");
        LogIn logIn = new LogIn(AccountType.PLAYER, accounts, passwords, tokens);

        IssuedToken token = logIn.logIn("  Player@Example.com ", PASSWORD);

        assertThat(tokens.read(token.value()))
                .contains(AuthenticatedPrincipal.global(playerId, AccountType.PLAYER, "PLAYER"));
        assertThat(token.timeToLive()).isEqualTo(OpaqueTokens.TIME_TO_LIVE);
    }

    @Test
    void wrongPasswordAndUnknownAccountFailTheSameWay() {
        accounts.add("player@example.com", PASSWORD, "PLAYER");
        LogIn logIn = new LogIn(AccountType.PLAYER, accounts, passwords, tokens);

        assertThatThrownBy(() -> logIn.logIn("player@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> logIn.logIn("nobody@example.com", PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void anUnknownAccountStillSpendsAPasswordCheck() {
        LogIn logIn = new LogIn(AccountType.PLATFORM, accounts, passwords, tokens);

        assertThatThrownBy(() -> logIn.logIn("nobody@example.com", PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(passwords.unknownAccountChecks()).isOne();
    }
}
