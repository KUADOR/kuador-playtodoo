package com.kuador.platodo.api.audit.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.kuador.platodo.api.audit.application.InvalidAuditPageException;
import com.kuador.platodo.api.audit.application.SupportComplexNotFoundException;
import com.kuador.platodo.api.audit.application.SupportSessionNotFoundException;
import com.kuador.platodo.api.audit.application.SupportSessionRefusedException;

/** Problem Details of {@link SupportAccessController}. */
@RestControllerAdvice(assignableTypes = SupportAccessController.class)
class AuditProblemHandler {

    @ExceptionHandler(SupportComplexNotFoundException.class)
    ProblemDetail complexNotFound() {
        return AuditProblem.COMPLEX_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(SupportSessionNotFoundException.class)
    ProblemDetail sessionNotFound() {
        return AuditProblem.SUPPORT_SESSION_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(SupportSessionRefusedException.class)
    ProblemDetail refused(SupportSessionRefusedException refusal) {
        return AuditProblem.of(refusal.code()).toProblem();
    }

    /** Malformed JSON, a malformed id or page parameter, or a page out of the contract limits. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        InvalidAuditPageException.class})
    ProblemDetail invalidRequest() {
        return AuditProblem.INVALID_REQUEST.toProblem();
    }
}
