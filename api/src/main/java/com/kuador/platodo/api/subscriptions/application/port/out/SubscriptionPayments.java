package com.kuador.platodo.api.subscriptions.application.port.out;

import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionPayment;

/** Recorded subscription payments. They are only inserted: a payment is never updated nor deleted. */
public interface SubscriptionPayments {

    void insert(SubscriptionPayment payment);

    /** One page of the payments of the complex {@code tenantId}: newest {@code paidOn} first, then newest recorded. */
    PaymentPage page(UUID tenantId, PageRequest page);

    record PaymentPage(List<SubscriptionPayment> items, long totalItems) {

        public PaymentPage {
            items = List.copyOf(items);
        }
    }
}
