package com.kuador.platodo.api.subscriptions.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.kuador.platodo.api.subscriptions.domain.LastPayment;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;

/** A row of the complex list: registry data, subscription and latest payment, if any. */
public record ComplexSummary(TenantProfile tenant, Subscription subscription, Optional<LastPayment> lastPayment) {

    public ComplexSummary {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(subscription, "subscription");
        Objects.requireNonNull(lastPayment, "lastPayment");
    }

    /** One page of the complex list, newest first. */
    public record Page(List<ComplexSummary> items, long totalItems) {

        public Page {
            items = List.copyOf(items);
        }
    }
}
