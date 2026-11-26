package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.List;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PlanPage;

/** Schema {@code PlanPage}. */
record PlanPageResponse(List<PlanResponse> items, int page, int size, long totalItems) {

    static PlanPageResponse of(PlanPage plans, PageRequest page) {
        return new PlanPageResponse(plans.items().stream().map(PlanResponse::of).toList(), page.page(), page.size(),
                plans.totalItems());
    }
}
