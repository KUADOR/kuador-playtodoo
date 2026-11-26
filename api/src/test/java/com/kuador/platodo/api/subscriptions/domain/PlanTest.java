package com.kuador.platodo.api.subscriptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.subscriptions.support.PlanFieldFixtures;

/** CA3: only a complete plan is published, and a published plan stays complete (ADR-018). */
class PlanTest {

    private static final PlanFieldDefinition SUPPORT_HOURS =
            PlanFieldFixtures.custom("Support hours", PlanFieldType.TEXT, true);
    private static final PlanFieldDefinition OPTIONAL_PERK =
            PlanFieldFixtures.custom("Optional perk", PlanFieldType.TEXT, false);
    private static final PlanFieldDefinition RETIRED_REQUIRED =
            PlanFieldFixtures.retired("Legacy perk", PlanFieldType.TEXT);
    private static final List<PlanFieldDefinition> FIELDS = fields();

    @Test
    void aNewPlanIsADraftAtVersionZero() {
        Plan plan = Plan.draft(UUID.randomUUID(), nameOnly());

        assertThat(plan.status()).isEqualTo(PlanStatus.DRAFT);
        assertThat(plan.version()).isZero();
    }

    @Test
    void publishingRequiresEveryBasicFieldAndEveryOfferedRequiredCustomField() {
        Plan draft = Plan.draft(UUID.randomUUID(), nameOnly());

        assertThatThrownBy(() -> draft.publish(FIELDS))
                .isInstanceOfSatisfying(PlanIncompleteException.class, incomplete ->
                        assertThat(incomplete.missingFields()).containsExactly(
                                new FieldError("price", FieldError.Code.REQUIRED),
                                new FieldError("currency", FieldError.Code.REQUIRED),
                                new FieldError("periodicity", FieldError.Code.REQUIRED),
                                new FieldError("courtLimit", FieldError.Code.REQUIRED),
                                new FieldError("userLimit", FieldError.Code.REQUIRED),
                                FieldError.ofCustomField("support-hours", FieldError.Code.REQUIRED)));
    }

    @Test
    void aCompletePlanIsPublishedOnceAndRepublishingChangesNothing() {
        Plan draft = Plan.draft(UUID.randomUUID(), complete());

        Plan published = draft.publish(FIELDS);

        assertThat(published.status()).isEqualTo(PlanStatus.PUBLISHED);
        assertThat(published.version()).isEqualTo(1);
        assertThat(published.publish(FIELDS)).isSameAs(published);
    }

    @Test
    void aDraftMayBeRevisedWithMissingFields() {
        Plan draft = Plan.draft(UUID.randomUUID(), complete());

        Plan revised = draft.revise(nameOnly(), FIELDS);

        assertThat(revised.status()).isEqualTo(PlanStatus.DRAFT);
        assertThat(revised.version()).isEqualTo(1);
        assertThat(revised.content()).isEqualTo(nameOnly());
    }

    @Test
    void aPublishedPlanStaysPublishedAndMustStayComplete() {
        Plan published = Plan.draft(UUID.randomUUID(), complete()).publish(FIELDS);
        PlanContent cheaper = new PlanContent("Premium", new BigDecimal("99.90"), "USD", "MONTHLY", 10, 25,
                Map.of("support-hours", "Weekdays"));

        Plan revised = published.revise(cheaper, FIELDS);

        assertThat(revised.status()).isEqualTo(PlanStatus.PUBLISHED);
        assertThat(revised.version()).isEqualTo(2);
        assertThatThrownBy(() -> published.revise(nameOnly(), FIELDS)).isInstanceOf(PlanIncompleteException.class);
    }

    private static PlanContent nameOnly() {
        return new PlanContent("Premium", null, null, null, null, null, Map.of());
    }

    private static PlanContent complete() {
        return new PlanContent("Premium", new BigDecimal("120.00"), "USD", "MONTHLY", 10, 25,
                Map.of("support-hours", "Weekdays 9 to 18"));
    }

    private static List<PlanFieldDefinition> fields() {
        List<PlanFieldDefinition> fields = new ArrayList<>(PlanFieldFixtures.basicFields());
        fields.addAll(List.of(SUPPORT_HOURS, OPTIONAL_PERK, RETIRED_REQUIRED));
        return fields;
    }
}
