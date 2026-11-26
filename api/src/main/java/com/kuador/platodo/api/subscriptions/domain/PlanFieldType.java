package com.kuador.platodo.api.subscriptions.domain;

import java.math.BigDecimal;

/** Type of a plan field: text, number or yes/no. Immutable once the field exists (ADR-018). */
public enum PlanFieldType {

    TEXT(String.class),
    NUMBER(BigDecimal.class),
    BOOLEAN(Boolean.class);

    private final Class<?> valueClass;

    PlanFieldType(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    /** Whether {@code value} is a value of this type: a {@link String}, a {@link BigDecimal} or a {@link Boolean}. */
    public boolean accepts(Object value) {
        return valueClass.isInstance(value);
    }
}
