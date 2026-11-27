package com.kuador.platodo.api.subscriptions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** PLATODO-27 CA7: a payment has a positive amount, a date that is not in the future and a known method. */
class PaymentDraftTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-12-10");

    @Test
    void aValidPaymentIsAcceptedUpToToday() {
        PaymentTerms terms = new PaymentDraft("120.5", TODAY, "TRANSFER").check(TODAY);

        assertThat(terms.amount()).isEqualByComparingTo(new BigDecimal("120.50"));
        assertThat(terms.paidOn()).isEqualTo(TODAY);
        assertThat(terms.method()).isEqualTo(PaymentMethod.TRANSFER);
    }

    @Test
    void everyInvalidFieldIsReportedTogether() {
        assertThatThrownBy(() -> new PaymentDraft("0", TODAY.plusDays(1), "CASH").check(TODAY))
                .isInstanceOfSatisfying(InvalidPaymentException.class, invalid -> assertThat(invalid.fieldErrors())
                        .containsExactlyInAnyOrder(
                                new FieldError("amount", FieldError.Code.NOT_POSITIVE),
                                new FieldError("paidOn", FieldError.Code.DATE_IN_FUTURE),
                                new FieldError("method", FieldError.Code.NOT_ALLOWED)));
    }

    @Test
    void missingFieldsAreRequired() {
        assertThatThrownBy(() -> new PaymentDraft(null, null, null).check(TODAY))
                .isInstanceOfSatisfying(InvalidPaymentException.class, invalid -> assertThat(invalid.fieldErrors())
                        .containsExactlyInAnyOrder(
                                new FieldError("amount", FieldError.Code.REQUIRED),
                                new FieldError("paidOn", FieldError.Code.REQUIRED),
                                new FieldError("method", FieldError.Code.REQUIRED)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1,5", "12.12345", "1234567890123456", "abc", ""})
    void anAmountOutsideTheMoneyFormatIsInvalid(String amount) {
        assertThatThrownBy(() -> new PaymentDraft(amount, TODAY, "INVOICE").check(TODAY))
                .isInstanceOfSatisfying(InvalidPaymentException.class, invalid -> assertThat(invalid.fieldErrors())
                        .containsExactly(new FieldError("amount", FieldError.Code.INVALID_FORMAT)));
    }
}
