package com.kuador.platodo.api.tenancy.application.port.out;

import com.kuador.platodo.api.tenancy.domain.event.TenantEvent;

/**
 * Publishes lifecycle events inside the current transaction. Consumers react after the commit, except for
 * {@link com.kuador.platodo.api.tenancy.domain.event.TenantPurged}, which they handle within it (ADR-013, D11).
 */
@FunctionalInterface
public interface TenantEventPublisher {

    void publish(TenantEvent event);
}
