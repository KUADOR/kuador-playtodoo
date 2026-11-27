package com.kuador.platodo.api.subscriptions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A payment as the backoffice sends it, before it is checked:
 * <ul>
 *   <li>{@code amount}: decimal text with up to 15 integer digits and 4 decimals, greater than zero.</li>
 *   <li>{@code paidOn}: not after {@code today}.</li>
 *   <li>{@code method}: one of {@link PaymentMethod}.</li>
 * </ul>
 */
public record PaymentDraft(String amount, LocalDate paidOn, String method) {

    private static final Pattern MONEY = Pattern.compile("^\\d{1,15}(\\.\\d{1,4})?$");
    private static final String AMOUNT = "amount";
    private static final String PAID_ON = "paidOn";
    private static final String METHOD = "method";

    /**
     * The checked terms of the payment.
     *
     * @param today the current date; a payment cannot be dated after it
     * @throws InvalidPaymentException with every invalid field
     */
    public PaymentTerms check(LocalDate today) {
        List<FieldError> errors = new ArrayList<>();
        Optional<BigDecimal> checkedAmount = checkAmount(errors);
        checkPaidOn(today, errors);
        Optional<PaymentMethod> checkedMethod = checkMethod(errors);
        if (!errors.isEmpty()) {
            throw new InvalidPaymentException(errors);
        }
        return new PaymentTerms(checkedAmount.orElseThrow(), paidOn, checkedMethod.orElseThrow());
    }

    private Optional<BigDecimal> checkAmount(List<FieldError> errors) {
        if (amount == null) {
            errors.add(new FieldError(AMOUNT, FieldError.Code.REQUIRED));
            return Optional.empty();
        }
        if (!MONEY.matcher(amount).matches()) {
            errors.add(new FieldError(AMOUNT, FieldError.Code.INVALID_FORMAT));
            return Optional.empty();
        }
        BigDecimal value = new BigDecimal(amount);
        if (value.signum() <= 0) {
            errors.add(new FieldError(AMOUNT, FieldError.Code.NOT_POSITIVE));
            return Optional.empty();
        }
        return Optional.of(value);
    }

    private void checkPaidOn(LocalDate today, List<FieldError> errors) {
        if (paidOn == null) {
            errors.add(new FieldError(PAID_ON, FieldError.Code.REQUIRED));
        } else if (paidOn.isAfter(today)) {
            errors.add(new FieldError(PAID_ON, FieldError.Code.DATE_IN_FUTURE));
        }
    }

    private Optional<PaymentMethod> checkMethod(List<FieldError> errors) {
        if (method == null) {
            errors.add(new FieldError(METHOD, FieldError.Code.REQUIRED));
            return Optional.empty();
        }
        Optional<PaymentMethod> known = PaymentMethod.named(method);
        if (known.isEmpty()) {
            errors.add(new FieldError(METHOD, FieldError.Code.NOT_ALLOWED));
        }
        return known;
    }
}
