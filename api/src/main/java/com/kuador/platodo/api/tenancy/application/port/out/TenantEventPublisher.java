package com.kuador.platodo.api.tenancy.application.port.out;

import com.kuador.platodo.api.tenancy.domain.event.TenantEvent;

/**
 * Publishes lifecycle events inside the current transaction. Consumers react after the commit, except for
 * {@link com.kuador.platodo.api.tenancy.domain.event.TenantPurged} and
 * {@link com.kuador.platodo.api.tenancy.domain.event.TenantStatusChanged}, which they handle within it (ADR-013 D11,
 * ADR-014).
 */
@FunctionalInterface
public interface TenantEventPublisher {

    void publish(TenantEvent event);
}
