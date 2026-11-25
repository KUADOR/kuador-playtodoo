package com.kuador.platodo.api.tenancy.application.port.out;

import com.kuador.platodo.api.tenancy.domain.event.TenantEvent;

/** Publishes lifecycle events inside the current transaction; consumers react after the commit. */
@FunctionalInterface
public interface TenantEventPublisher {

    void publish(TenantEvent event);
}
