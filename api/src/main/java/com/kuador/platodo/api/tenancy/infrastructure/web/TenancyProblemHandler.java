package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders the rejections of {@link TenantResolutionFilter} as Problem Details. It applies to every handler because
 * the filter rejects before a handler is chosen.
 */
@RestControllerAdvice
class TenancyProblemHandler {

    @ExceptionHandler(TenantRequestRejectedException.class)
    ProblemDetail rejected(TenantRequestRejectedException rejection) {
        return rejection.problem().toProblem();
    }
}
