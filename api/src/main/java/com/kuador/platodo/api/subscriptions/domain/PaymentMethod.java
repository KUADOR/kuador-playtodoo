package com.kuador.platodo.api.subscriptions.domain;

import java.util.Arrays;
import java.util.Optional;

/** How a complex paid its subscription, outside the platform (ADR-004). */
public enum PaymentMethod {
    TRANSFER,
    INVOICE;

    /** The method named {@code name}; empty when there is none. */
    public static Optional<PaymentMethod> named(String name) {
        return Arrays.stream(values()).filter(method -> method.name().equals(name)).findFirst();
    }
}
