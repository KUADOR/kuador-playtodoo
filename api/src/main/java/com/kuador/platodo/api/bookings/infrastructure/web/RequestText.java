package com.kuador.platodo.api.bookings.infrastructure.web;

/**
 * Normalizes free-text fields of request bodies: surrounding spaces are removed and an over-long value is
 * rejected, so the request fails as malformed before reaching the database.
 */
final class RequestText {

    private RequestText() {
    }

    /** @throws IllegalArgumentException if the value is missing, blank or longer than {@code maxLength} */
    static String required(String value, String field, int maxLength) {
        String text = optional(value, field, maxLength);
        if (text == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return text;
    }

    /**
     * @return the stripped value, or null when it is missing or blank
     * @throws IllegalArgumentException if the value is longer than {@code maxLength}
     */
    static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.strip();
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(field + " must have at most " + maxLength + " characters");
        }
        return text;
    }
}
