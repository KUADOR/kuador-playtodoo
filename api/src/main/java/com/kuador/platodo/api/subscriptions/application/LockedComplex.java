package com.kuador.platodo.api.subscriptions.application;

import java.util.Objects;

import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;

/** A complex whose subscription and tenant row are locked by the current global transaction. */
public record LockedComplex(Subscription subscription, TenantProfile tenant) {

    public LockedComplex {
        Objects.requireNonNull(subscription, "subscription");
        Objects.requireNonNull(tenant, "tenant");
    }
}
