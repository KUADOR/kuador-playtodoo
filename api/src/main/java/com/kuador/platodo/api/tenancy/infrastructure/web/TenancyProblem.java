package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Problem Details answered by Tenancy: tenant resolution, the platform access guard, the lifecycle and the subdomain
 * operations ({@code contracts/tenancy.openapi.yaml}). Title and detail are developer-facing; clients map {@code code}
 * to the text shown to people. Details never name the host, the state or personal data.
 */
enum TenancyProblem {

    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "tenant-not-found", "Complex not found",
            "No operating complex answers on this host."),
    TENANT_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "tenant-not-found", "Complex not found",
            "No complex has this id.", "TENANT_NOT_FOUND"),
    TENANT_UNAVAILABLE(HttpStatus.FORBIDDEN, "tenant-unavailable", "Complex unavailable",
            "The complex of this host is not available."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "resource-not-found", "Resource not found",
            "This operation is not served on this host."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentication required",
            "A valid access token is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden", "Not allowed",
            "The account is not allowed to perform this operation."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request",
            "Malformed body or parameters."),
    SUBDOMAIN_TAKEN(HttpStatus.CONFLICT, "subdomain-taken", "Subdomain taken",
            "The subdomain is used, or was used, by a complex."),
    SUBDOMAIN_INVALID(HttpStatus.UNPROCESSABLE_CONTENT, "subdomain-invalid", "Invalid subdomain",
            "Use 3 to 63 lower-case letters, digits or single hyphens, not at the ends."),
    SUBDOMAIN_RESERVED(HttpStatus.UNPROCESSABLE_CONTENT, "subdomain-reserved", "Reserved subdomain",
            "The subdomain is reserved by the platform."),
    INVALID_TRANSITION(HttpStatus.CONFLICT, "invalid-transition", "Invalid lifecycle transition",
            "The tenant is not in a state that allows this operation."),
    PROVISIONING_INCOMPLETE(HttpStatus.CONFLICT, "invalid-transition", "Invalid lifecycle transition",
            "The owner account of the complex is missing, so its provisioning cannot be completed.",
            "INVALID_TRANSITION"),
    ACCESS_SESSION_INVALID(HttpStatus.FORBIDDEN, "access-session-invalid", "Access session invalid",
            "A valid access session for this complex is required.");

    private static final String TYPE_BASE = "https://playtodoo.com/problems/";
    private static final String CODE = "code";

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final String detail;
    private final String code;

    TenancyProblem(HttpStatus status, String slug, String title, String detail) {
        this(status, slug, title, detail, null);
    }

    /** @param code the contract code, when it differs from the constant name; null to use the name */
    TenancyProblem(HttpStatus status, String slug, String title, String detail, String code) {
        this.status = status;
        this.type = URI.create(TYPE_BASE + slug);
        this.title = title;
        this.detail = detail;
        this.code = code == null ? name() : code;
    }

    ProblemDetail toProblem() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setProperty(CODE, code);
        return problem;
    }
}
