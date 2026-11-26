package com.kuador.platodo.api.subscriptions.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.port.out.LastPayments;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions.SubscriptionPage;
import com.kuador.platodo.api.subscriptions.domain.LastPayment;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * {@code GET /complexes}: every platform user; newest first. A page costs three queries whatever its size (plus the
 * count): the subscriptions, their tenants through {@link TenantDirectory} in one batch, and their latest payments.
 */
public final class ListComplexes {

    private final CapabilityGuard guard;
    private final Subscriptions subscriptions;
    private final TenantDirectory directory;
    private final LastPayments lastPayments;

    public ListComplexes(CapabilityGuard guard, Subscriptions subscriptions, TenantDirectory directory,
            LastPayments lastPayments) {
        this.guard = guard;
        this.subscriptions = subscriptions;
        this.directory = directory;
        this.lastPayments = lastPayments;
    }

    public ComplexSummary.Page list(PageRequest page) {
        guard.requirePlatformUser();
        SubscriptionPage subscriptionPage = subscriptions.page(page);
        List<UUID> ids = subscriptionPage.items().stream().map(Subscription::tenantId).toList();
        Map<UUID, TenantProfile> tenants = directory.profiles(ids.stream().map(id -> new TenantId(id.toString()))
                        .toList())
                .stream()
                .collect(Collectors.toMap(tenant -> UUID.fromString(tenant.id().value()), Function.identity()));
        Map<UUID, LastPayment> payments = lastPayments.latestOf(ids);
        List<ComplexSummary> items = subscriptionPage.items().stream()
                .map(subscription -> summaryOf(subscription, tenants, payments))
                .toList();
        return new ComplexSummary.Page(items, subscriptionPage.totalItems());
    }

    private static ComplexSummary summaryOf(Subscription subscription, Map<UUID, TenantProfile> tenants,
            Map<UUID, LastPayment> payments) {
        TenantProfile tenant = tenants.get(subscription.tenantId());
        if (tenant == null) {
            throw new IllegalStateException("Subscription without tenant: " + subscription.tenantId());
        }
        return new ComplexSummary(tenant, subscription, Optional.ofNullable(payments.get(subscription.tenantId())));
    }
}
