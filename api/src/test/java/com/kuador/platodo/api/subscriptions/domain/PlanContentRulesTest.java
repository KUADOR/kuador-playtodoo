package com.kuador.platodo.api.subscriptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.subscriptions.support.PlanFieldFixtures;

/** Validation of the plan fields against the plan options and the field definitions (ADR-018). */
class PlanContentRulesTest {

    private static final PlanOptions OPTIONS = new PlanOptions(List.of("USD"), List.of("MONTHLY", "YEARLY"));
    private static final PlanFieldDefinition SUPPORT_HOURS =
            PlanFieldFixtures.custom("Support hours", PlanFieldType.TEXT, false);
    private static final PlanFieldDefinition MAX_TOURNAMENTS =
            PlanFieldFixtures.custom("Max tournaments", PlanFieldType.NUMBER, false);
    private static final PlanFieldDefinition TOURNAMENTS_INCLUDED =
            PlanFieldFixtures.custom("Tournaments included", PlanFieldType.BOOLEAN, false);
    private static final PlanFieldDefinition LEGACY_PERK = PlanFieldFixtures.retired("Legacy perk", PlanFieldType.TEXT);

    private final PlanContentRules rules = new PlanContentRules(OPTIONS, fields());

    @Test
    void aDraftNeedsOnlyAName() {
        PlanContent content = rules.check(new PlanDraft("  Premium ", null, null, null, null, null, null));

        assertThat(content.name()).isEqualTo("Premium");
        assertThat(content.price()).isNull();
        assertThat(content.customFields()).isEmpty();
    }

    @Test
    void acceptsCompleteFieldsWithACustomValueOfEachType() {
        PlanContent content = rules.check(new PlanDraft("Premium", "120.00", "USD", "MONTHLY", 10, 25, Map.of(
                "support-hours", " Weekdays 9 to 18 ",
                "max-tournaments", new BigDecimal("4"),
                "tournaments-included", true)));

        assertThat(content.price()).isEqualByComparingTo("120");
        assertThat(content.currency()).isEqualTo("USD");
        assertThat(content.periodicity()).isEqualTo("MONTHLY");
        assertThat(content.customFields()).containsOnly(
                Map.entry("max-tournaments", new BigDecimal("4")),
                Map.entry("support-hours", "Weekdays 9 to 18"),
                Map.entry("tournaments-included", true));
    }

    @Test
    void reportsEveryInvalidBasicFieldAtOnce() {
        PlanDraft draft = new PlanDraft(" ", "12.34567", "EUR", "WEEKLY", -1, -2, null);

        assertInvalid(draft, Map.of(),
                new FieldError("name", FieldError.Code.REQUIRED),
                new FieldError("price", FieldError.Code.INVALID_FORMAT),
                new FieldError("currency", FieldError.Code.NOT_ALLOWED),
                new FieldError("periodicity", FieldError.Code.NOT_ALLOWED),
                new FieldError("courtLimit", FieldError.Code.NEGATIVE),
                new FieldError("userLimit", FieldError.Code.NEGATIVE));
    }

    @Test
    void rejectsTooLongNamesAndMalformedPrices() {
        assertInvalid(draftNamed("x".repeat(PlanContentRules.MAX_NAME_LENGTH + 1)), Map.of(),
                new FieldError("name", FieldError.Code.TOO_LONG));
        for (String price : List.of("-1", "1e3", "1,5", "1234567890123456", "abc", "")) {
            assertInvalid(new PlanDraft("Premium", price, null, null, null, null, null), Map.of(),
                    new FieldError("price", FieldError.Code.INVALID_FORMAT));
        }
    }

    @Test
    void customValuesMustMatchTheFieldType() {
        assertInvalid(withCustom(Map.of("tournaments-included", "yes", "max-tournaments", "4",
                        "support-hours", true)), Map.of(),
                FieldError.ofCustomField("max-tournaments", FieldError.Code.WRONG_TYPE),
                FieldError.ofCustomField("support-hours", FieldError.Code.WRONG_TYPE),
                FieldError.ofCustomField("tournaments-included", FieldError.Code.WRONG_TYPE));
        assertInvalid(withCustom(Map.of("support-hours",
                        "x".repeat(PlanContentRules.MAX_TEXT_VALUE_LENGTH + 1))), Map.of(),
                FieldError.ofCustomField("support-hours", FieldError.Code.TOO_LONG));
    }

    @Test
    void unknownKeysAndBasicKeysAreNotCustomFields() {
        assertInvalid(withCustom(Map.of("unknown", "x", "price", "10")), Map.of(),
                FieldError.ofCustomField("price", FieldError.Code.UNKNOWN_FIELD),
                FieldError.ofCustomField("unknown", FieldError.Code.UNKNOWN_FIELD));
    }

    @Test
    void nullOrBlankValuesClearACustomField() {
        Map<String, Object> custom = new HashMap<>();
        custom.put("support-hours", "  ");
        custom.put("max-tournaments", null);

        PlanContent content = rules.check(withCustom(custom), Map.of("support-hours", "Old value"));

        assertThat(content.customFields()).isEmpty();
    }

    @Test
    void retiredFieldsKeepTheirStoredValueAndCannotBeChanged() {
        Map<String, Object> stored = Map.of("legacy-perk", "Free balls", "support-hours", "Old value");

        PlanContent omitted = rules.check(withCustom(Map.of()), stored);
        PlanContent sentBack = rules.check(withCustom(Map.of("legacy-perk", "Free balls")), stored);

        assertThat(omitted.customFields()).containsOnly(Map.entry("legacy-perk", "Free balls"));
        assertThat(sentBack.customFields()).containsOnly(Map.entry("legacy-perk", "Free balls"));
        assertInvalid(withCustom(Map.of("legacy-perk", "Other")), stored,
                FieldError.ofCustomField("legacy-perk", FieldError.Code.NOT_ALLOWED));
        assertInvalid(withCustom(Map.of("legacy-perk", "New")), Map.of(),
                FieldError.ofCustomField("legacy-perk", FieldError.Code.NOT_ALLOWED));
    }

    @Test
    void aRetiredNumberSentBackWithAnotherScaleIsTheSameValue() {
        PlanFieldDefinition retiredNumber = PlanFieldFixtures.retired("Old limit", PlanFieldType.NUMBER);
        PlanContentRules withRetiredNumber = new PlanContentRules(OPTIONS, List.of(retiredNumber));

        PlanContent content = withRetiredNumber.check(withCustom(Map.of("old-limit", new BigDecimal("3.0"))),
                Map.of("old-limit", new BigDecimal("3")));

        assertThat(content.customFields()).containsOnly(Map.entry("old-limit", new BigDecimal("3")));
    }

    private void assertInvalid(PlanDraft draft, Map<String, Object> stored, FieldError... expected) {
        assertThatThrownBy(() -> rules.check(draft, stored))
                .isInstanceOfSatisfying(InvalidPlanFieldsException.class, invalid ->
                        assertThat(invalid.fieldErrors()).containsExactlyInAnyOrder(expected));
    }

    private static PlanDraft draftNamed(String name) {
        return new PlanDraft(name, null, null, null, null, null, null);
    }

    private static PlanDraft withCustom(Map<String, Object> custom) {
        return new PlanDraft("Premium", null, null, null, null, null, custom);
    }

    private static List<PlanFieldDefinition> fields() {
        List<PlanFieldDefinition> fields = new ArrayList<>(PlanFieldFixtures.basicFields());
        fields.addAll(List.of(SUPPORT_HOURS, MAX_TOURNAMENTS, TOURNAMENTS_INCLUDED, LEGACY_PERK));
        return fields;
    }
}
