package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.out.AccountDirectory;
import com.kuador.platodo.api.identity.domain.EmailAddress;

import jakarta.persistence.EntityManager;

/** Kuador platform users, through the global persistence unit. */
public final class JpaPlatformUsers implements AccountDirectory {

    private final EntityManager global;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public JpaPlatformUsers(EntityManager global) {
        this.global = global;
    }

    @Override
    public Optional<StoredAccount> findActiveByEmail(EmailAddress email) {
        return global.createQuery("""
                        SELECT u FROM PlatformUserEntity u WHERE u.email = :email AND u.status = :active
                        """, PlatformUserEntity.class)
                .setParameter("email", email.value())
                .setParameter("active", AccountStatus.ACTIVE)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .map(user -> new StoredAccount(user.id(), user.passwordHash(), user.role()));
    }

    @Override
    public boolean isActive(UUID accountId) {
        return global.createQuery("""
                        SELECT count(u) FROM PlatformUserEntity u WHERE u.id = :id AND u.status = :active
                        """, Long.class)
                .setParameter("id", accountId)
                .setParameter("active", AccountStatus.ACTIVE)
                .getSingleResult() > 0;
    }
}
