package com.kuador.platodo.api.subscriptions.application;

import java.util.Objects;
import java.util.Set;

import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionStatus;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;

/**
 * A complex as the backoffice shows it: its registry data in Tenancy and its subscription here.
 *
 * @param statusChangedByName visible name of who set the current subscription status
 * @param version             of the complex as a whole: the sum of the tenant and subscription versions, which both
 *                            only grow, so any change to either one changes it
 */
public record ComplexDetail(TenantProfile tenant, Subscription subscription, String statusChangedByName,
        Set<SubscriptionStatus> allowedTransitions, long version) {

    public ComplexDetail {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(subscription, "subscription");
        Objects.requireNonNull(statusChangedByName, "statusChangedByName");
        allowedTransitions = Set.copyOf(allowedTransitions);
    }
}
