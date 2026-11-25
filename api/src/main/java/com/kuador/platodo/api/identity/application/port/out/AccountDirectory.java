package com.kuador.platodo.api.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.identity.domain.EmailAddress;

/**
 * Accounts of one kind where they live: staff in the schema of the current tenant, players and Kuador users in the
 * global schema. Only active accounts are visible.
 */
public interface AccountDirectory {

    Optional<StoredAccount> findActiveByEmail(EmailAddress email);

    boolean isActive(UUID accountId);

    /** What a login needs from an account; the hash never leaves Identity and Access. */
    record StoredAccount(UUID id, String passwordHash, String role) {
    }
}
