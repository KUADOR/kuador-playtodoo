package com.kuador.platodo.api.subscriptions.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.domain.Subscription;

/** Stored subscriptions, one per complex. Writes must run inside a global transaction. */
public interface Subscriptions {

    void insert(Subscription subscription);

    /** The subscription of the complex {@code tenantId}. */
    Optional<Subscription> findByTenant(UUID tenantId);

    /** One page of subscriptions, newest first. */
    SubscriptionPage page(PageRequest page);

    record SubscriptionPage(List<Subscription> items, long totalItems) {

        public SubscriptionPage {
            items = List.copyOf(items);
        }
    }
}
