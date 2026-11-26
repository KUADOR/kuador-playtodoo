package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.Plan;
import com.kuador.platodo.api.subscriptions.domain.PlanContent;

/**
 * Schema {@code Plan}. The price is decimal text with at least {@value #MIN_PRICE_DECIMALS} decimals and no trailing
 * zeros beyond them ({@code 120.00}, {@code 120.5} as {@code 120.50}, {@code 9.1234}), never a floating-point number.
 */
record PlanResponse(UUID id, String name, String price, String currency, String periodicity, Integer courtLimit,
        Integer userLimit, Map<String, Object> customFields, String status, long version) {

    private static final int MIN_PRICE_DECIMALS = 2;

    static PlanResponse of(Plan plan) {
        PlanContent content = plan.content();
        return new PlanResponse(plan.id(), content.name(), priceText(content.price()), content.currency(),
                content.periodicity(), content.courtLimit(), content.userLimit(), content.customFields(),
                plan.status().name(), plan.version());
    }

    private static String priceText(BigDecimal price) {
        if (price == null) {
            return null;
        }
        BigDecimal stripped = price.stripTrailingZeros();
        int scale = Math.max(MIN_PRICE_DECIMALS, stripped.scale());
        return stripped.setScale(scale, RoundingMode.UNNECESSARY).toPlainString();
    }
}
