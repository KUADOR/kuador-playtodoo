package com.kuador.platodo.api.subscriptions.infrastructure.events;

import org.springframework.context.ApplicationEventPublisher;

import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionEvents;
import com.kuador.platodo.api.subscriptions.domain.event.SubscriptionEvent;

/**
 * Publishes the events of Plans and subscriptions as Spring application events (ADR-007), in-process, same thread
 * and transaction. {@code SubscriptionStatusChanged} consumers listen with a plain {@code @EventListener} to apply its
 * effect inside the change transaction (ADR-014); {@code SubscriptionPlanChanged} consumers listen with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)}.
 */
public final class SpringSubscriptionEvents implements SubscriptionEvents {

    private final ApplicationEventPublisher publisher;

    public SpringSubscriptionEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(SubscriptionEvent event) {
        publisher.publishEvent(event);
    }
}
