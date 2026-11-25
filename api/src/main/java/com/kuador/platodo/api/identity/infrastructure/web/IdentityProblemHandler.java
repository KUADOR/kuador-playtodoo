package com.kuador.platodo.api.identity.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kuador.platodo.api.identity.application.InvalidCredentialsException;

/** Problem Details of the login operations. */
@RestControllerAdvice(assignableTypes = LoginController.class)
class IdentityProblemHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials() {
        return IdentityProblem.INVALID_CREDENTIALS.toProblem();
    }

    /** Malformed JSON, or a {@link LoginRequest} that breaks the contract limits. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail invalidRequest() {
        return IdentityProblem.INVALID_REQUEST.toProblem();
    }
}
