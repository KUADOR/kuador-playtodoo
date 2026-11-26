package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.List;

import com.kuador.platodo.api.subscriptions.domain.PlanOptions;

/** Schema {@code PlanOptions}. */
record PlanOptionsResponse(List<String> currencies, List<String> periodicities) {

    static PlanOptionsResponse of(PlanOptions options) {
        return new PlanOptionsResponse(options.currencies(), options.periodicities());
    }
}
