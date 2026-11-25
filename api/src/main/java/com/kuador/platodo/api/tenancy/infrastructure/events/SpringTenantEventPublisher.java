package com.kuador.platodo.api.tenancy.infrastructure.events;

import org.springframework.context.ApplicationEventPublisher;

import com.kuador.platodo.api.tenancy.application.port.out.TenantEventPublisher;
import com.kuador.platodo.api.tenancy.domain.event.TenantEvent;

/**
 * Publishes lifecycle events as Spring application events (ADR-007), in-process, same thread and transaction.
 * Consumers listen with {@code @TransactionalEventListener(phase = AFTER_COMMIT)}, except for {@code TenantPurged},
 * whose consumers listen with a plain {@code @EventListener} to run inside the purge transaction (ADR-013, D11).
 */
public final class SpringTenantEventPublisher implements TenantEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringTenantEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(TenantEvent event) {
        publisher.publishEvent(event);
    }
}
