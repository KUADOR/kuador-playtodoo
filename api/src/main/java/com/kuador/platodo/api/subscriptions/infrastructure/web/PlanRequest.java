package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.LinkedHashMap;
import java.util.Map;

import com.kuador.platodo.api.subscriptions.domain.PlanDraft;

import tools.jackson.databind.JsonNode;

/**
 * Schemas {@code PlanInput} and {@code UpdatePlanRequest}. Custom values keep their JSON type: strings, numbers and
 * booleans become {@link String}, {@link java.math.BigDecimal} and {@link Boolean}; any other JSON value is passed on
 * unchanged so the plan rules report it as the wrong type.
 */
record PlanRequest(String name, String price, String currency, String periodicity, Integer courtLimit,
        Integer userLimit, JsonNode customFields, Long version) {

    PlanDraft toDraft() {
        return new PlanDraft(name, price, currency, periodicity, courtLimit, userLimit, customValues());
    }

    long requiredVersion() {
        if (version == null || version < 0) {
            throw new InvalidPlansRequestException("`version` is required and must not be negative.");
        }
        return version;
    }

    private Map<String, Object> customValues() {
        if (customFields == null || customFields.isNull()) {
            return Map.of();
        }
        if (!customFields.isObject()) {
            throw new InvalidPlansRequestException("`customFields` must be an object.");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        customFields.properties().forEach(property -> values.put(property.getKey(), valueOf(property.getValue())));
        return values;
    }

    private static Object valueOf(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (node.isString()) {
            return node.asString();
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node;
    }
}
