package com.kuador.platodo.api.subscriptions.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A commercial plan (ADR-018). A draft needs only its name. Publishing requires every basic field and every required
 * custom field that is still offered, and a published plan must stay complete when it is edited. Every change raises
 * {@code version} by one; publishing an already published plan changes nothing.
 */
public record Plan(UUID id, PlanContent content, PlanStatus status, long version) {

    public Plan {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(status, "status");
    }

    public static Plan draft(UUID id, PlanContent content) {
        return new Plan(id, content, PlanStatus.DRAFT, 0);
    }

    /**
     * The plan with {@code newContent}, which replaces every field.
     *
     * @throws PlanIncompleteException if the plan is published and {@code newContent} leaves a required field empty
     */
    public Plan revise(PlanContent newContent, List<PlanFieldDefinition> fields) {
        Plan revised = new Plan(id, newContent, status, version + 1);
        if (isPublished()) {
            revised.requireComplete(fields);
        }
        return revised;
    }

    /**
     * The plan published; itself when it already was.
     *
     * @throws PlanIncompleteException if a basic field or a required custom field is empty
     */
    public Plan publish(List<PlanFieldDefinition> fields) {
        if (isPublished()) {
            return this;
        }
        requireComplete(fields);
        return new Plan(id, content, PlanStatus.PUBLISHED, version + 1);
    }

    public boolean isPublished() {
        return status == PlanStatus.PUBLISHED;
    }

    private void requireComplete(List<PlanFieldDefinition> fields) {
        List<FieldError> missing = new ArrayList<>();
        requirePresent(content.price(), "price", missing);
        requirePresent(content.currency(), "currency", missing);
        requirePresent(content.periodicity(), "periodicity", missing);
        requirePresent(content.courtLimit(), "courtLimit", missing);
        requirePresent(content.userLimit(), "userLimit", missing);
        fields.stream()
                .filter(field -> field.isOffered() && field.required())
                .filter(field -> !content.customFields().containsKey(field.key()))
                .forEach(field -> missing.add(FieldError.ofCustomField(field.key(), FieldError.Code.REQUIRED)));
        if (!missing.isEmpty()) {
            throw new PlanIncompleteException(missing);
        }
    }

    private static void requirePresent(Object value, String field, List<FieldError> missing) {
        if (value == null) {
            missing.add(new FieldError(field, FieldError.Code.REQUIRED));
        }
    }
}
