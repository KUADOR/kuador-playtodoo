package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.Map;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.Plan;
import com.kuador.platodo.api.subscriptions.domain.PlanContent;

/** Schema {@code Plan}. The price follows {@link MoneyText}. */
record PlanResponse(UUID id, String name, String price, String currency, String periodicity, Integer courtLimit,
        Integer userLimit, Map<String, Object> customFields, String status, long version) {

    static PlanResponse of(Plan plan) {
        PlanContent content = plan.content();
        return new PlanResponse(plan.id(), content.name(), MoneyText.of(content.price()), content.currency(),
                content.periodicity(), content.courtLimit(), content.userLimit(), content.customFields(),
                plan.status().name(), plan.version());
    }
}
