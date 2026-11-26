package com.kuador.platodo.api.subscriptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Field rules of {@code RegisterComplexRequest}: they answer 422 VALIDATION_FAILED with every invalid field. */
class ComplexRegistrationTest {

    private static final UUID PLAN = UUID.randomUUID();

    @Test
    void theNameIsTrimmedAndTheEmailNormalized() {
        ComplexRegistration registration =
                ComplexRegistration.of("  Canchas El Sol ", "canchas-el-sol", " Owner@Example.COM ", PLAN);

        assertThat(registration.displayName()).isEqualTo("Canchas El Sol");
        assertThat(registration.ownerEmail()).isEqualTo("owner@example.com");
        assertThat(registration.subdomain()).isEqualTo("canchas-el-sol");
    }

    @Test
    void missingFieldsAreReportedTogether() {
        assertThatThrownBy(() -> ComplexRegistration.of("  ", "canchas-el-sol", null, PLAN))
                .isInstanceOfSatisfying(InvalidComplexRegistrationException.class, invalid ->
                        assertThat(invalid.fieldErrors()).containsExactly(
                                new FieldError("displayName", FieldError.Code.REQUIRED),
                                new FieldError("ownerEmail", FieldError.Code.REQUIRED)));
    }

    @Test
    void aNameLongerThanTheLimitIsTooLong() {
        String tooLong = "x".repeat(ComplexRegistration.MAX_DISPLAY_NAME_LENGTH + 1);

        assertThatThrownBy(() -> ComplexRegistration.of(tooLong, "canchas-el-sol", "owner@example.com", PLAN))
                .isInstanceOfSatisfying(InvalidComplexRegistrationException.class, invalid ->
                        assertThat(invalid.fieldErrors()).containsExactly(
                                new FieldError("displayName", FieldError.Code.TOO_LONG)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner", "owner@", "@example.com", "owner@example", "own er@example.com", "a@b@c.com"})
    void aMalformedEmailIsReported(String email) {
        assertThatThrownBy(() -> ComplexRegistration.of("Canchas El Sol", "canchas-el-sol", email, PLAN))
                .isInstanceOfSatisfying(InvalidComplexRegistrationException.class, invalid ->
                        assertThat(invalid.fieldErrors()).containsExactly(
                                new FieldError("ownerEmail", FieldError.Code.INVALID_FORMAT)));
    }
}
