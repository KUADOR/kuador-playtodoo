package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Schema {@code Money}: decimal text with at least {@value #MIN_DECIMALS} decimals and no trailing zeros beyond them
 * ({@code 120.00}, {@code 120.5} as {@code 120.50}, {@code 9.1234}), never a floating-point number.
 */
final class MoneyText {

    private static final int MIN_DECIMALS = 2;

    private MoneyText() {
    }

    /** Null for a null amount. */
    static String of(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal stripped = amount.stripTrailingZeros();
        int scale = Math.max(MIN_DECIMALS, stripped.scale());
        return stripped.setScale(scale, RoundingMode.UNNECESSARY).toPlainString();
    }
}
