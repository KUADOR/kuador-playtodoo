package com.kuador.platodo.api.identity.application;

import java.util.Objects;
import java.util.Optional;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens.IssuedToken;
import com.kuador.platodo.api.identity.application.port.out.AccountDirectory;
import com.kuador.platodo.api.identity.application.port.out.AccountDirectory.StoredAccount;
import com.kuador.platodo.api.identity.application.port.out.PasswordHashes;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.identity.domain.EmailAddress;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

/**
 * Logs in one kind of account against its own directory. A staff login runs on the host of a complex and only sees
 * the staff of that complex, so credentials of complex A on the host of complex B fail exactly like unknown ones.
 */
public final class LogIn {

    private final AccountType type;
    private final AccountDirectory accounts;
    private final PasswordHashes passwords;
    private final AccessTokens tokens;

    public LogIn(AccountType type, AccountDirectory accounts, PasswordHashes passwords, AccessTokens tokens) {
        this.type = Objects.requireNonNull(type, "type");
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    /** @throws InvalidCredentialsException if no active account matches the e-mail and the password */
    public IssuedToken logIn(String email, String password) {
        Optional<StoredAccount> account = accounts.findActiveByEmail(EmailAddress.normalize(email));
        if (account.isEmpty()) {
            passwords.matchNothing(password);
            throw new InvalidCredentialsException();
        }
        if (!passwords.matches(password, account.get().passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokens.issue(principalOf(account.get()));
    }

    private AuthenticatedPrincipal principalOf(StoredAccount account) {
        return type == AccountType.STAFF
                ? AuthenticatedPrincipal.staff(account.id(), TenantScope.require(), account.role())
                : AuthenticatedPrincipal.global(account.id(), type, account.role());
    }
}
