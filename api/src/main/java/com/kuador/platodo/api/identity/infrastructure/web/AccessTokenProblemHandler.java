package com.kuador.platodo.api.identity.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders the rejections of {@link AccessTokenFilter}. It applies to every handler because the filter rejects before
 * a handler is chosen.
 */
@RestControllerAdvice
class AccessTokenProblemHandler {

    @ExceptionHandler(AccessTokenRejectedException.class)
    ProblemDetail rejected(AccessTokenRejectedException rejection) {
        return rejection.problem().toProblem();
    }
}
