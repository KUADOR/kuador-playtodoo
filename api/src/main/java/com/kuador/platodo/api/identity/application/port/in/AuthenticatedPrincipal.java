package com.kuador.platodo.api.identity.application.port.in;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * The account a request acts as, read from an accepted access token.
 *
 * @param accountId id of the account (claim {@code sub})
 * @param type      kind of account (claim {@code typ})
 * @param tenant    complex of a {@link AccountType#STAFF} account (claim {@code tid}); null for any other type
 * @param role      role of the account (claim {@code role})
 */
public record AuthenticatedPrincipal(UUID accountId, AccountType type, TenantId tenant, String role) {

    public AuthenticatedPrincipal {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(role, "role");
        if ((type == AccountType.STAFF) != (tenant != null)) {
            throw new IllegalArgumentException("Only staff accounts belong to a tenant, and they always do");
        }
    }

    public static AuthenticatedPrincipal staff(UUID accountId, TenantId tenant, String role) {
        return new AuthenticatedPrincipal(accountId, AccountType.STAFF, Objects.requireNonNull(tenant, "tenant"), role);
    }

    public static AuthenticatedPrincipal global(UUID accountId, AccountType type, String role) {
        return new AuthenticatedPrincipal(accountId, type, null, role);
    }

    /** The complex of a staff account; empty for players and Kuador. */
    public Optional<TenantId> staffTenant() {
        return Optional.ofNullable(tenant);
    }

    public boolean is(AccountType expected) {
        return type == expected;
    }
}
