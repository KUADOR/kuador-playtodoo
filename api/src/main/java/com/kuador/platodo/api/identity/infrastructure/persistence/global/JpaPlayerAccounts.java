package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.out.AccountDirectory;
import com.kuador.platodo.api.identity.domain.EmailAddress;

import jakarta.persistence.EntityManager;

/** Global player accounts, through the global persistence unit. */
public final class JpaPlayerAccounts implements AccountDirectory {

    /** Players have no role of their own; the token carries this one. */
    static final String PLAYER_ROLE = "PLAYER";

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaPlayerAccounts(EntityManager global) {
        this.global = global;
    }

    @Override
    public Optional<StoredAccount> findActiveByEmail(EmailAddress email) {
        return global.createQuery("""
                        SELECT p FROM PlayerAccountEntity p WHERE p.email = :email AND p.status = :active
                        """, PlayerAccountEntity.class)
                .setParameter("email", email.value())
                .setParameter("active", AccountStatus.ACTIVE)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .map(player -> new StoredAccount(player.id(), player.passwordHash(), PLAYER_ROLE));
    }

    @Override
    public boolean isActive(UUID accountId) {
        return global.createQuery("""
                        SELECT count(p) FROM PlayerAccountEntity p WHERE p.id = :id AND p.status = :active
                        """, Long.class)
                .setParameter("id", accountId)
                .setParameter("active", AccountStatus.ACTIVE)
                .getSingleResult() > 0;
    }
}
