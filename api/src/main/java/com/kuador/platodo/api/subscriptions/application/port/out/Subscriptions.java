package com.kuador.platodo.api.subscriptions.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.domain.Subscription;

/**
 * Stored subscriptions, one per complex. Writes and locks must run inside a global transaction. A change of a complex
 * locks its subscription first and then its tenant row ({@code TenantProfileLock}), always in that order.
 */
public interface Subscriptions {

    void insert(Subscription subscription);

    /** The subscription of the complex {@code tenantId}, locked until the current transaction ends. */
    Optional<Subscription> lockByTenant(UUID tenantId);

    /** Stores the plan, status and version of a subscription that was locked in the current transaction. */
    void update(Subscription subscription);

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
