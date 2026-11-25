package com.kuador.platodo.api.audit.infrastructure.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Problem Details of the platform access operations ({@code contracts/tenancy.openapi.yaml}). Title and detail are
 * developer-facing; clients map {@code code} to the text shown to people.
 */
enum AuditProblem {

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentication required",
            "A valid access token is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden", "Not allowed",
            "Only the Kuador platform owner may use this operation."),
    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "tenant-not-found", "Complex not found", "No tenant with this id."),
    ACCESS_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "access-session-not-found", "Access session not found",
            "No access session with this id for the caller."),
    REASON_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "reason-required", "Reason required",
            "An access session needs a non-blank reason."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", "Malformed body or parameters.");

    private static final String TYPE_BASE = "https://playtodoo.com/problems/";
    private static final String CODE = "code";

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final String detail;

    AuditProblem(HttpStatus status, String slug, String title, String detail) {
        this.status = status;
        this.type = URI.create(TYPE_BASE + slug);
        this.title = title;
        this.detail = detail;
    }

    ProblemDetail toProblem() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setProperty(CODE, name());
        return problem;
    }
}
