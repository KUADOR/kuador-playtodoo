package com.kuador.platodo.api.subscriptions.application;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * Reads the detail of one complex from its subscription and from {@link TenantDirectory}; it never reads a tenant
 * schema. Shared by the operations that answer a {@code ComplexDetail}; authorizing the caller is theirs.
 */
public final class ComplexDetails {

    /** Tenant states in which the subscription can change: provisioned and not being purged. */
    private static final Set<TenantStatus> OPEN_TO_COMMERCIAL_CHANGES =
            EnumSet.of(TenantStatus.ACTIVE, TenantStatus.SUSPENDED, TenantStatus.ARCHIVED);

    private final Subscriptions subscriptions;
    private final TenantDirectory directory;
    private final PlatformUserNames userNames;
    private final Clock clock;

    public ComplexDetails(Subscriptions subscriptions, TenantDirectory directory, PlatformUserNames userNames,
            Clock clock) {
        this.subscriptions = subscriptions;
        this.directory = directory;
        this.userNames = userNames;
        this.clock = clock;
    }

    /** @throws ComplexNotFoundException if no complex with a subscription has this id */
    public ComplexDetail of(UUID complexId) {
        Subscription subscription = subscriptions.findByTenant(complexId).orElseThrow(ComplexNotFoundException::new);
        TenantProfile tenant = directory.profiles(List.of(new TenantId(complexId.toString()))).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Subscription without tenant: " + complexId));
        String changedBy = userNames.displayNameOf(subscription.statusChangedBy())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown platform user " + subscription.statusChangedBy()));
        return new ComplexDetail(tenant, subscription, changedBy,
                subscription.allowedTransitions(acceptsCommercialChanges(tenant, clock.instant())),
                tenant.version() + subscription.version());
    }

    /** Throws {@link ComplexNotFoundException} unless a complex with a subscription has this id. */
    void requireExists(UUID complexId) {
        if (subscriptions.findByTenant(complexId).isEmpty()) {
            throw new ComplexNotFoundException();
        }
    }

    private static boolean acceptsCommercialChanges(TenantProfile tenant, Instant now) {
        boolean retentionExpired = tenant.purgeDueAt() != null && !now.isBefore(tenant.purgeDueAt());
        return OPEN_TO_COMMERCIAL_CHANGES.contains(tenant.status()) && !retentionExpired;
    }
}
