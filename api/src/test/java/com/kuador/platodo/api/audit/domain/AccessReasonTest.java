package com.kuador.platodo.api.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AccessReasonTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    void aMissingOrBlankReasonIsRequired(String blank) {
        assertThatThrownBy(() -> new AccessReason(blank)).isInstanceOf(ReasonRequiredException.class);
    }

    @Test
    void theReasonIsTrimmed() {
        assertThat(new AccessReason("  Duplicated booking complaint  ").value()).isEqualTo("Duplicated booking complaint");
    }

    @Test
    void aReasonLongerThanTheLimitIsRejected() {
        assertThat(new AccessReason("x".repeat(AccessReason.MAX_LENGTH)).value()).hasSize(AccessReason.MAX_LENGTH);
        assertThatThrownBy(() -> new AccessReason("x".repeat(AccessReason.MAX_LENGTH + 1)))
                .isInstanceOf(ReasonTooLongException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    void parsingAMissingOrBlankReasonGivesNone(String blank) {
        assertThat(AccessReason.parse(blank)).isEmpty();
    }

    @Test
    void parsingKeepsTheLimitAndTrims() {
        assertThat(AccessReason.parse("  Ticket 1234  ")).contains(new AccessReason("Ticket 1234"));
        assertThat(AccessReason.parse("x".repeat(AccessReason.MAX_LENGTH))).isPresent();
        assertThat(AccessReason.parse("x".repeat(AccessReason.MAX_LENGTH + 1))).isEmpty();
    }
}
