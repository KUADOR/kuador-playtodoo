package com.kuador.platodo.api.audit.infrastructure.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import com.kuador.platodo.api.audit.domain.DenialCode;

/**
 * Problem Details of the support access operations ({@code contracts/platform-backoffice.openapi.yaml}). Title and
 * detail are developer-facing; the backoffice maps {@code code} to the text shown to people. {@code UNAUTHENTICATED}
 * and {@code FORBIDDEN} are answered by Identity and access for every platform operation.
 */
enum AuditProblem {

    COMPLEX_NOT_FOUND(HttpStatus.NOT_FOUND, "complex-not-found", "Complex not found", "No complex with this id."),
    SUPPORT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "support-session-not-found", "Support session not found",
            "No support session with this id for the caller."),
    CAPABILITY_REVOKED(HttpStatus.FORBIDDEN, "capability-revoked", "Capability not enabled",
            "Operators do not have the capability this operation needs."),
    TENANT_NOT_ACCESSIBLE(HttpStatus.FORBIDDEN, "tenant-not-accessible", "Complex not accessible",
            "The status of the complex does not allow the role of the caller."),
    REASON_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "reason-required", "Reason required",
            "A support session needs a reason of 1 to 500 characters."),
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

    /** The problem answered when the opening of a session is refused with {@code code}. */
    static AuditProblem of(DenialCode code) {
        return switch (code) {
            case CAPABILITY_REVOKED -> CAPABILITY_REVOKED;
            case TENANT_NOT_ACCESSIBLE -> TENANT_NOT_ACCESSIBLE;
            case REASON_REQUIRED -> REASON_REQUIRED;
            case SUPPORT_SESSION_INVALID ->
                    throw new IllegalArgumentException("SUPPORT_SESSION_INVALID is answered on tenant hosts only");
        };
    }

    ProblemDetail toProblem() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setProperty(CODE, name());
        return problem;
    }
}
