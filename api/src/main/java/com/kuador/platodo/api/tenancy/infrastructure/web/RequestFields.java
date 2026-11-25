package com.kuador.platodo.api.tenancy.infrastructure.web;

/** Limits of the request fields of {@code contracts/tenancy.openapi.yaml}, checked while a body is read. */
final class RequestFields {

    /** Longest subdomain candidate a request may carry; rule violations below it answer 422, not 400. */
    static final int MAX_SUBDOMAIN_CANDIDATE_LENGTH = 255;

    private RequestFields() {
    }

    /** @throws InvalidRequestException if {@code value} is missing, blank or longer than {@code maxLength} */
    static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(field + " is required");
        }
        if (value.length() > maxLength) {
            throw new InvalidRequestException(field + " is longer than " + maxLength + " characters");
        }
        return value;
    }

    /** A subdomain candidate: present and not absurdly long. Its rules are checked by Tenancy afterwards. */
    static String requireSubdomainCandidate(String value) {
        return requireText(value, "subdomain", MAX_SUBDOMAIN_CANDIDATE_LENGTH);
    }
}
