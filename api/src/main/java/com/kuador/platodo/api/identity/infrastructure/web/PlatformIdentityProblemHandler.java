package com.kuador.platodo.api.identity.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.kuador.platodo.api.identity.application.InvalidCapabilityChangeException;
import com.kuador.platodo.api.identity.application.StaleOperatorCapabilitiesException;

/** Problem Details of the operations of {@link PlatformIdentityController}, besides the guard denials. */
@RestControllerAdvice(assignableTypes = PlatformIdentityController.class)
class PlatformIdentityProblemHandler {

    @ExceptionHandler(StaleOperatorCapabilitiesException.class)
    ProblemDetail staleState() {
        return PlatformProblem.STALE_STATE.toProblem();
    }

    @ExceptionHandler({InvalidPlatformRequestException.class, InvalidCapabilityChangeException.class})
    ProblemDetail invalidRequest(RuntimeException invalid) {
        return PlatformProblem.INVALID_REQUEST.toProblem(invalid.getMessage());
    }

    /** Malformed JSON, an unknown capability key, or a query parameter that is not a number. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malformed() {
        return PlatformProblem.INVALID_REQUEST.toProblem();
    }
}
