package com.kuador.platodo.api.identity.infrastructure.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Problem Details of the platform operations of Identity and access
 * ({@code contracts/platform-backoffice.openapi.yaml}). Title and detail are developer-facing; the backoffice maps {@code code} to the text shown to people.
 */
enum PlatformProblem {

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentication required",
            "A valid platform access token is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden", "Not allowed",
            "The caller may not use this operation."),
    CAPABILITY_REVOKED(HttpStatus.FORBIDDEN, "capability-revoked", "Capability not enabled",
            "Operators do not have the capability this operation needs."),
    STALE_STATE(HttpStatus.CONFLICT, "stale-state", "Stale state",
            "The resource changed since it was read."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request",
            "Malformed body or parameters.");

    private static final String TYPE_BASE = "https://playtodoo.com/problems/";
    private static final String CODE = "code";

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final String detail;

    PlatformProblem(HttpStatus status, String slug, String title, String detail) {
        this.status = status;
        this.type = URI.create(TYPE_BASE + slug);
        this.title = title;
        this.detail = detail;
    }

    ProblemDetail toProblem() {
        return toProblem(detail);
    }

    /** With a detail of this occurrence; it must never contain personal data. */
    ProblemDetail toProblem(String occurrenceDetail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, occurrenceDetail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setProperty(CODE, name());
        return problem;
    }
}
