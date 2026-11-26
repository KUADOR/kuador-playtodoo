package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The jsonb object of custom field values. Values are written as JSON strings, numbers and booleans and read back as
 * {@link String}, {@link java.math.BigDecimal} and {@link Boolean}, the value types of the plan domain.
 */
final class CustomFieldsJson {

    private final JsonMapper json;

    CustomFieldsJson(JsonMapper json) {
        this.json = json;
    }

    String write(Map<String, Object> values) {
        return json.writeValueAsString(values);
    }

    Map<String, Object> read(String text) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> property : json.readTree(text).properties()) {
            values.put(property.getKey(), valueOf(property.getKey(), property.getValue()));
        }
        return values;
    }

    private static Object valueOf(String key, JsonNode node) {
        if (node.isString()) {
            return node.asString();
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        throw new IllegalStateException("Stored custom field " + key + " is not a text, number or boolean");
    }
}
