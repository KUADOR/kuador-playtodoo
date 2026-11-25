package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Problem Details answered by tenant resolution ({@code contracts/tenancy.openapi.yaml}). Title and detail are
 * developer-facing; clients map {@code code} to the text shown to people. Details never name the host or the state.
 */
enum TenancyProblem {

    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "tenant-not-found", "Complex not found",
            "No operating complex answers on this host."),
    TENANT_UNAVAILABLE(HttpStatus.FORBIDDEN, "tenant-unavailable", "Complex unavailable",
            "The complex of this host is not available."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "resource-not-found", "Resource not found",
            "This operation is not served on this host.");

    private static final String TYPE_BASE = "https://playtodoo.com/problems/";
    private static final String CODE = "code";

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final String detail;

    TenancyProblem(HttpStatus status, String slug, String title, String detail) {
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
