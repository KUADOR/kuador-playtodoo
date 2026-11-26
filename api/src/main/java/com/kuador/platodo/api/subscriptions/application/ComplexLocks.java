package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantProfileLock;

/**
 * Optimistic concurrency of the mutations on a complex: locks its subscription and then its tenant row (always in
 * that order) and compares the version the caller read, {@code tenant.version + subscription.version}, with both rows
 * locked. Must be called inside a global transaction.
 */
public final class ComplexLocks {

    private final Subscriptions subscriptions;
    private final TenantProfileLock tenants;

    public ComplexLocks(Subscriptions subscriptions, TenantProfileLock tenants) {
        this.subscriptions = subscriptions;
        this.tenants = tenants;
    }

    /**
     * @throws ComplexNotFoundException if no complex with a subscription has this id
     * @throws StaleVersionException    if {@code expectedVersion} is not the current version of the complex
     */
    public LockedComplex lockAt(UUID complexId, long expectedVersion) {
        Subscription subscription = subscriptions.lockByTenant(complexId).orElseThrow(ComplexNotFoundException::new);
        TenantProfile tenant = tenants.lock(new TenantId(complexId.toString()))
                .orElseThrow(() -> new IllegalStateException("Subscription without tenant: " + complexId));
        if (tenant.version() + subscription.version() != expectedVersion) {
            throw new StaleVersionException();
        }
        return new LockedComplex(subscription, tenant);
    }
}
