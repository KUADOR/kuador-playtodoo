package com.kuador.platodo.api.subscriptions.domain;

import java.util.Objects;

/**
 * One invalid or missing field. {@code field} is the property path of the contract, such as {@code price} or
 * {@code customFields.support-hours}.
 */
public record FieldError(String field, Code code) {

    private static final String CUSTOM_FIELDS_PREFIX = "customFields.";

    /** Stable reasons the backoffice maps to its own text. */
    public enum Code {
        REQUIRED,
        TOO_LONG,
        INVALID_FORMAT,
        NEGATIVE,
        NOT_POSITIVE,
        DATE_IN_FUTURE,
        NOT_ALLOWED,
        WRONG_TYPE,
        UNKNOWN_FIELD
    }

    public FieldError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(code, "code");
    }

    public static FieldError ofCustomField(String key, Code code) {
        return new FieldError(CUSTOM_FIELDS_PREFIX + key, code);
    }
}
