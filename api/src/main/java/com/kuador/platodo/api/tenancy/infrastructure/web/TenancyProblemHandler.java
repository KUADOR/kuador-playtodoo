package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders the rejections of {@link TenantResolutionFilter} and {@link SupportAccessGuard} as Problem Details. It
 * applies to every handler because both reject before the handler runs.
 */
@RestControllerAdvice
class TenancyProblemHandler {

    @ExceptionHandler(TenantRequestRejectedException.class)
    ProblemDetail rejected(TenantRequestRejectedException rejection) {
        return rejection.problem().toProblem();
    }
}
