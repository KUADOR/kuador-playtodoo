package com.kuador.platodo.api.subscriptions.application;

import java.util.List;
import java.util.Objects;

import com.kuador.platodo.api.subscriptions.domain.SubscriptionPayment;

/** A payment with the visible name of who recorded it. */
public record RecordedPayment(SubscriptionPayment payment, String recordedByName) {

    public RecordedPayment {
        Objects.requireNonNull(payment, "payment");
        Objects.requireNonNull(recordedByName, "recordedByName");
    }

    /** One page of the payments of a complex, newest first. */
    public record Page(List<RecordedPayment> items, long totalItems) {

        public Page {
            items = List.copyOf(items);
        }
    }
}
