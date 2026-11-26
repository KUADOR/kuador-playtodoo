package com.kuador.platodo.api.identity.infrastructure.events;

import org.springframework.context.ApplicationEventPublisher;

import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilityEvents;
import com.kuador.platodo.api.identity.domain.event.OperatorCapabilitiesChanged;

/**
 * Publishes {@link OperatorCapabilitiesChanged} as a Spring application event (ADR-007), in-process, same thread and
 * transaction. Consumers listen with {@code @TransactionalEventListener(phase = AFTER_COMMIT)}.
 */
public final class SpringOperatorCapabilityEvents implements OperatorCapabilityEvents {

    private final ApplicationEventPublisher publisher;

    public SpringOperatorCapabilityEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(OperatorCapabilitiesChanged event) {
        publisher.publishEvent(event);
    }
}
