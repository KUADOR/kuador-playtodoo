package com.kuador.platodo.api.identity.infrastructure.web;

/** {@code LoginRequest} of the contract. */
record LoginRequest(String email, String password) {

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PASSWORD_LENGTH = 128;

    /** @throws IllegalArgumentException if a field is missing, blank or longer than the contract allows */
    LoginRequest {
        requireText(email, "email", MAX_EMAIL_LENGTH);
        requireText(password, "password", MAX_PASSWORD_LENGTH);
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is longer than " + maxLength + " characters");
        }
    }
}
