package com.kuador.platodo.api.identity.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.CapabilityRevokedException;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerNotAuthenticatedException;

/**
 * Renders the denials of {@link CapabilityGuard} for every handler, so each module that guards a platform operation
 * answers the same {@code 401} and {@code 403} codes of the contract.
 */
@RestControllerAdvice
class CapabilityGuardProblemHandler {

    @ExceptionHandler(PlatformCallerNotAuthenticatedException.class)
    ProblemDetail unauthenticated() {
        return PlatformProblem.UNAUTHENTICATED.toProblem();
    }

    @ExceptionHandler(PlatformCallerForbiddenException.class)
    ProblemDetail forbidden() {
        return PlatformProblem.FORBIDDEN.toProblem();
    }

    @ExceptionHandler(CapabilityRevokedException.class)
    ProblemDetail capabilityRevoked() {
        return PlatformProblem.CAPABILITY_REVOKED.toProblem();
    }
}
