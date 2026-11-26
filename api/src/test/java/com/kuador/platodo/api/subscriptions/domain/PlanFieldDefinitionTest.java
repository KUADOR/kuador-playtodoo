package com.kuador.platodo.api.subscriptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.kuador.platodo.api.subscriptions.support.PlanFieldFixtures;

/** CA4: custom plan fields are created, edited and retired as data; basic fields never change (ADR-018). */
class PlanFieldDefinitionTest {

    private static final Instant NOW = Instant.parse("2027-01-15T10:00:00Z");

    @ParameterizedTest
    @CsvSource({
        "Support hours, support-hours",
        "  Horas de atención  , horas-de-atencion",
        "Límite de torneos (anual), limite-de-torneos-anual",
        "Wi-Fi / Parking, wi-fi-parking"})
    void aCustomFieldKeyIsDerivedFromItsLabel(String label, String key) {
        PlanFieldDefinition field = PlanFieldDefinition.custom(UUID.randomUUID(), label, PlanFieldType.TEXT, false);

        assertThat(field.key()).isEqualTo(key);
        assertThat(field.label()).isEqualTo(label.strip());
        assertThat(field.origin()).isEqualTo(PlanFieldOrigin.CUSTOM);
        assertThat(field.isRetired()).isFalse();
        assertThat(field.version()).isZero();
    }

    @Test
    void aLabelMustHaveTextWithinTheLimitAndALetterOrDigit() {
        assertFieldError(() -> PlanFieldFixtures.custom("   ", PlanFieldType.TEXT, false), FieldError.Code.REQUIRED);
        assertFieldError(() -> PlanFieldFixtures.custom("x".repeat(PlanFieldDefinition.MAX_LABEL_LENGTH + 1),
                PlanFieldType.TEXT, false), FieldError.Code.TOO_LONG);
        assertFieldError(() -> PlanFieldFixtures.custom("¡¿?!", PlanFieldType.TEXT, false),
                FieldError.Code.INVALID_FORMAT);
    }

    @Test
    void relabellingKeepsTheKeyAndTypeAndRaisesTheVersion() {
        PlanFieldDefinition field = PlanFieldFixtures.custom("Support hours", PlanFieldType.TEXT, false);

        PlanFieldDefinition relabelled = field.relabel("Support schedule", true);

        assertThat(relabelled.key()).isEqualTo("support-hours");
        assertThat(relabelled.type()).isEqualTo(PlanFieldType.TEXT);
        assertThat(relabelled.label()).isEqualTo("Support schedule");
        assertThat(relabelled.required()).isTrue();
        assertThat(relabelled.version()).isEqualTo(1);
    }

    @Test
    void relabellingWithTheSameValuesChangesNothing() {
        PlanFieldDefinition field = PlanFieldFixtures.custom("Support hours", PlanFieldType.TEXT, false);

        assertThat(field.relabel(" Support hours ", false)).isSameAs(field);
    }

    @Test
    void retiringRecordsWhenAndIsIdempotent() {
        PlanFieldDefinition field = PlanFieldFixtures.custom("Support hours", PlanFieldType.TEXT, true);

        PlanFieldDefinition retired = field.retire(NOW);

        assertThat(retired.retiredAt()).contains(NOW);
        assertThat(retired.isOffered()).isFalse();
        assertThat(retired.version()).isEqualTo(1);
        assertThat(retired.retire(NOW.plusSeconds(60))).isSameAs(retired);
    }

    @Test
    void basicFieldsAreReadOnlyAndNeverRetired() {
        PlanFieldDefinition price = PlanFieldFixtures.basic("price", PlanFieldType.NUMBER);

        assertThatThrownBy(() -> price.relabel("Cost", true)).isInstanceOf(BasicFieldReadOnlyException.class);
        assertThatThrownBy(() -> price.retire(NOW)).isInstanceOf(BasicFieldReadOnlyException.class);
        assertThat(price.isOffered()).isFalse();
    }

    @Test
    void eachTypeAcceptsOnlyItsValues() {
        assertThat(PlanFieldType.TEXT.accepts("Weekdays")).isTrue();
        assertThat(PlanFieldType.TEXT.accepts(new BigDecimal("1"))).isFalse();
        assertThat(PlanFieldType.NUMBER.accepts(new BigDecimal("2.5"))).isTrue();
        assertThat(PlanFieldType.NUMBER.accepts("2.5")).isFalse();
        assertThat(PlanFieldType.BOOLEAN.accepts(Boolean.TRUE)).isTrue();
        assertThat(PlanFieldType.BOOLEAN.accepts("true")).isFalse();
    }

    private static void assertFieldError(Runnable action, FieldError.Code code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(InvalidPlanFieldsException.class, invalid ->
                        assertThat(invalid.fieldErrors()).containsExactly(new FieldError("label", code)));
    }
}
