package com.kuador.platodo.api.identity.infrastructure.persistence.tenant;

import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.out.AccountDirectory;
import com.kuador.platodo.api.identity.domain.EmailAddress;

import jakarta.persistence.EntityManager;

/**
 * Staff of the complex in {@code TenantScope}, through the tenant persistence unit: only that complex's schema is
 * read, and without a scope every query fails closed.
 */
public final class JpaStaffUsers implements AccountDirectory {

    private static final String ACTIVE = "ACTIVE";

    private final EntityManager tenant;

    /** {@code tenant} must be an entity manager of the tenant persistence unit. */
    public JpaStaffUsers(EntityManager tenant) {
        this.tenant = tenant;
    }

    @Override
    public Optional<StoredAccount> findActiveByEmail(EmailAddress email) {
        return tenant.createQuery("""
                        SELECT s FROM StaffUserEntity s WHERE s.email = :email AND s.status = :active
                        """, StaffUserEntity.class)
                .setParameter("email", email.value())
                .setParameter("active", ACTIVE)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .map(staff -> new StoredAccount(staff.id(), staff.passwordHash(), staff.role()));
    }

    @Override
    public boolean isActive(UUID accountId) {
        return tenant.createQuery("""
                        SELECT count(s) FROM StaffUserEntity s WHERE s.id = :id AND s.status = :active
                        """, Long.class)
                .setParameter("id", accountId)
                .setParameter("active", ACTIVE)
                .getSingleResult() > 0;
    }
}
