package com.kuador.platodo.api.audit.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.kuador.platodo.api.audit.application.AccessSessionNotFoundException;
import com.kuador.platodo.api.audit.application.AccessTenantNotFoundException;
import com.kuador.platodo.api.audit.application.InvalidAuditPageException;
import com.kuador.platodo.api.audit.application.NotPlatformOwnerException;
import com.kuador.platodo.api.audit.application.PlatformUserNotAuthenticatedException;
import com.kuador.platodo.api.audit.domain.ReasonRequiredException;
import com.kuador.platodo.api.audit.domain.ReasonTooLongException;

/** Problem Details of {@link PlatformAccessController}. */
@RestControllerAdvice(assignableTypes = PlatformAccessController.class)
class AuditProblemHandler {

    @ExceptionHandler(PlatformUserNotAuthenticatedException.class)
    ProblemDetail unauthenticated() {
        return AuditProblem.UNAUTHENTICATED.toProblem();
    }

    @ExceptionHandler(NotPlatformOwnerException.class)
    ProblemDetail forbidden() {
        return AuditProblem.FORBIDDEN.toProblem();
    }

    @ExceptionHandler(AccessTenantNotFoundException.class)
    ProblemDetail tenantNotFound() {
        return AuditProblem.TENANT_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(AccessSessionNotFoundException.class)
    ProblemDetail sessionNotFound() {
        return AuditProblem.ACCESS_SESSION_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(ReasonRequiredException.class)
    ProblemDetail reasonRequired() {
        return AuditProblem.REASON_REQUIRED.toProblem();
    }

    /** Malformed JSON, a reason over the contract limit, a malformed id or page parameter. */
    @ExceptionHandler({HttpMessageNotReadableException.class, ReasonTooLongException.class,
        MethodArgumentTypeMismatchException.class, InvalidAuditPageException.class})
    ProblemDetail invalidRequest() {
        return AuditProblem.INVALID_REQUEST.toProblem();
    }
}
