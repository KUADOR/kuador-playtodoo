package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import com.kuador.platodo.api.subscriptions.domain.FieldError;

/**
 * Problem Details of the plan and complex operations ({@code contracts/platform-backoffice.openapi.yaml}). Titles are
 * developer-facing; the backoffice maps {@code code} and each field error code to the text shown to people. The
 * subdomain rejections of a registration (PLATODO-27 CA2) also carry their Spanish message in {@code detail}, so any
 * client can show it as is.
 */
enum SubscriptionsProblem {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", "Malformed body or parameters."),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, "validation-failed", "Validation failed",
            "One or more fields are invalid."),
    PLAN_INCOMPLETE(HttpStatus.UNPROCESSABLE_CONTENT, "plan-incomplete", "Plan incomplete",
            "Basic or required fields are missing."),
    BASIC_FIELD_READ_ONLY(HttpStatus.UNPROCESSABLE_CONTENT, "basic-field-read-only", "Basic field is read-only",
            "Basic plan fields cannot be changed nor retired."),
    STALE_STATE(HttpStatus.CONFLICT, "stale-state", "Stale state", "The resource changed since it was read."),
    PLAN_NAME_TAKEN(HttpStatus.CONFLICT, "plan-name-taken", "Plan name taken",
            "Another plan has this name."),
    FIELD_NAME_TAKEN(HttpStatus.CONFLICT, "field-name-taken", "Field name taken",
            "Another plan field has this label."),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "plan-not-found", "Plan not found", "No plan with this id."),
    PLAN_FIELD_NOT_FOUND(HttpStatus.NOT_FOUND, "plan-field-not-found", "Plan field not found",
            "No plan field with this id."),
    COMPLEX_NOT_FOUND(HttpStatus.NOT_FOUND, "complex-not-found", "Complex not found", "No complex with this id."),
    SUBDOMAIN_TAKEN(HttpStatus.CONFLICT, "subdomain-taken", "Subdomain taken",
            "El subdominio ya está en uso o lo usó otro complejo; elige otro."),
    SUBDOMAIN_INVALID(HttpStatus.UNPROCESSABLE_CONTENT, "subdomain-invalid", "Invalid subdomain",
            "El subdominio no es válido: usa de 3 a 63 letras minúsculas, números o guiones simples, sin guion al"
                    + " inicio ni al final."),
    SUBDOMAIN_RESERVED(HttpStatus.UNPROCESSABLE_CONTENT, "subdomain-reserved", "Reserved subdomain",
            "El subdominio está reservado por Playtodoo; elige otro."),
    PLAN_NOT_PUBLISHED(HttpStatus.UNPROCESSABLE_CONTENT, "plan-not-published", "Plan not published",
            "The plan does not exist or is a draft."),
    NOT_PROVISIONING_FAILED(HttpStatus.CONFLICT, "not-provisioning-failed", "Provisioning did not fail",
            "The complex is not waiting for a provisioning retry.");

    private static final String TYPE_BASE = "https://playtodoo.com/problems/";
    private static final String CODE = "code";
    private static final String FIELD_ERRORS = "fieldErrors";
    private static final String FIELD = "field";

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final String detail;

    SubscriptionsProblem(HttpStatus status, String slug, String title, String detail) {
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

    ProblemDetail toProblem(List<FieldError> fieldErrors) {
        ProblemDetail problem = toProblem();
        problem.setProperty(FIELD_ERRORS, fieldErrors.stream()
                .map(error -> Map.of(FIELD, error.field(), CODE, error.code().name()))
                .toList());
        return problem;
    }
}
