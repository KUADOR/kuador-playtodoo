package com.kuador.platodo.api.identity.application.port.out;

import com.kuador.platodo.api.identity.domain.event.OperatorCapabilitiesChanged;

/** Publishes {@link OperatorCapabilitiesChanged} inside the current transaction; consumers react after the commit. */
@FunctionalInterface
public interface OperatorCapabilityEvents {

    void publish(OperatorCapabilitiesChanged event);
}
