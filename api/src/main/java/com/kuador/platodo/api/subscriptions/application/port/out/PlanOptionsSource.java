package com.kuador.platodo.api.subscriptions.application.port.out;

import com.kuador.platodo.api.subscriptions.domain.PlanOptions;

/** The allowed currencies and periodicities, read from the platform parameters on every call. */
public interface PlanOptionsSource {

    PlanOptions current();
}
