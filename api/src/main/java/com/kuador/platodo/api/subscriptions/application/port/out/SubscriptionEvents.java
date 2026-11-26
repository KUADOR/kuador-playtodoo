package com.kuador.platodo.api.subscriptions.application.port.out;

import com.kuador.platodo.api.subscriptions.domain.event.SubscriptionEvent;

/** Publishes the events of the module in-process, in the thread and transaction of the caller (ADR-007). */
@FunctionalInterface
public interface SubscriptionEvents {

    void publish(SubscriptionEvent event);
}
