package com.kuador.platodo.api.identity.support;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kuador.platodo.api.identity.application.port.out.AccountDirectory;
import com.kuador.platodo.api.identity.domain.EmailAddress;

/** Active accounts of one kind, by normalized e-mail; the stored "hash" is the password itself. */
public final class InMemoryAccounts implements AccountDirectory {

    private final Map<String, StoredAccount> byEmail = new ConcurrentHashMap<>();

    public UUID add(String email, String password, String role) {
        UUID id = UUID.randomUUID();
        byEmail.put(EmailAddress.normalize(email).value(), new StoredAccount(id, password, role));
        return id;
    }

    public void disable(UUID accountId) {
        byEmail.values().removeIf(account -> account.id().equals(accountId));
    }

    @Override
    public Optional<StoredAccount> findActiveByEmail(EmailAddress email) {
        return Optional.ofNullable(byEmail.get(email.value()));
    }

    @Override
    public boolean isActive(UUID accountId) {
        return byEmail.values().stream().anyMatch(account -> account.id().equals(accountId));
    }
}
