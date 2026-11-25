package com.kuador.platodo.api.identity.infrastructure.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Problem Details answered by Identity and Access ({@code contracts/tenancy.openapi.yaml}). Title and detail are
 * developer-facing; clients map {@code code} to the text shown to people. Details never say whether an account exists.
 */
enum IdentityProblem {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid credentials",
            "The e-mail or the password is not valid."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentication required",
            "A valid access token is required."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request",
            "Malformed body or parameters.");

    private static final String TYPE_BASE = "https://playtodoo.com/problems/";
    private static final String CODE = "code";

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final String detail;

    IdentityProblem(HttpStatus status, String slug, String title, String detail) {
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
