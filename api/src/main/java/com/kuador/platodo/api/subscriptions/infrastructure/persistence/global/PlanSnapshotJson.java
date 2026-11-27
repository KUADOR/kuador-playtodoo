package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.PlanSnapshot;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The jsonb object of {@code subscription.plan_snapshot}. The price is kept as decimal text and the instant as
 * ISO-8601 text, so neither loses precision; custom fields are written like {@code plan.custom_fields}.
 */
final class PlanSnapshotJson {

    private static final String PLAN_ID = "planId";
    private static final String NAME = "name";
    private static final String PRICE = "price";
    private static final String CURRENCY = "currency";
    private static final String PERIODICITY = "periodicity";
    private static final String COURT_LIMIT = "courtLimit";
    private static final String USER_LIMIT = "userLimit";
    private static final String CUSTOM_FIELDS = "customFields";
    private static final String ASSIGNED_AT = "assignedAt";

    private final JsonMapper json;
    private final CustomFieldsJson customFields;

    PlanSnapshotJson(JsonMapper json) {
        this.json = json;
        this.customFields = new CustomFieldsJson(json);
    }

    String write(PlanSnapshot snapshot) {
        ObjectNode node = json.createObjectNode();
        node.put(PLAN_ID, snapshot.planId().toString());
        node.put(NAME, snapshot.name());
        node.put(PRICE, snapshot.price().toPlainString());
        node.put(CURRENCY, snapshot.currency());
        node.put(PERIODICITY, snapshot.periodicity());
        node.put(COURT_LIMIT, snapshot.courtLimit());
        node.put(USER_LIMIT, snapshot.userLimit());
        node.set(CUSTOM_FIELDS, json.readTree(customFields.write(snapshot.customFields())));
        node.put(ASSIGNED_AT, snapshot.assignedAt().toString());
        return json.writeValueAsString(node);
    }

    PlanSnapshot read(String text) {
        JsonNode node = json.readTree(text);
        return new PlanSnapshot(
                UUID.fromString(node.get(PLAN_ID).asString()),
                node.get(NAME).asString(),
                new BigDecimal(node.get(PRICE).asString()),
                node.get(CURRENCY).asString(),
                node.get(PERIODICITY).asString(),
                node.get(COURT_LIMIT).asInt(),
                node.get(USER_LIMIT).asInt(),
                customFields.read(json.writeValueAsString(node.get(CUSTOM_FIELDS))),
                Instant.parse(node.get(ASSIGNED_AT).asString()));
    }
}
