package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kuador.platodo.api.tenancy.application.CallerNotAllowedException;
import com.kuador.platodo.api.tenancy.application.CallerNotAuthenticatedException;
import com.kuador.platodo.api.tenancy.application.ProvisioningIncompleteException;
import com.kuador.platodo.api.tenancy.application.SubdomainTakenException;
import com.kuador.platodo.api.tenancy.application.TenantNotFoundException;
import com.kuador.platodo.api.tenancy.application.TenantUnavailableException;
import com.kuador.platodo.api.tenancy.domain.InvalidTransitionException;
import com.kuador.platodo.api.tenancy.domain.RetentionExpiredException;
import com.kuador.platodo.api.tenancy.domain.RetentionInvalidException;
import com.kuador.platodo.api.tenancy.domain.SubdomainRejectedException;

/** Problem Details of the lifecycle and subdomain operations of {@code contracts/tenancy.openapi.yaml}. */
@RestControllerAdvice(assignableTypes = {
        PlatformTenantController.class, SubdomainController.class, TenantSubdomainController.class})
class TenancyOperationsProblemHandler {

    @ExceptionHandler(CallerNotAuthenticatedException.class)
    ProblemDetail unauthenticated() {
        return TenancyProblem.UNAUTHENTICATED.toProblem();
    }

    @ExceptionHandler(CallerNotAllowedException.class)
    ProblemDetail forbidden() {
        return TenancyProblem.FORBIDDEN.toProblem();
    }

    @ExceptionHandler(TenantNotFoundException.class)
    ProblemDetail tenantNotFound() {
        return TenancyProblem.TENANT_ID_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(TenantUnavailableException.class)
    ProblemDetail tenantUnavailable() {
        return TenancyProblem.TENANT_UNAVAILABLE.toProblem();
    }

    @ExceptionHandler(SubdomainTakenException.class)
    ProblemDetail subdomainTaken() {
        return TenancyProblem.SUBDOMAIN_TAKEN.toProblem();
    }

    @ExceptionHandler(SubdomainRejectedException.class)
    ProblemDetail subdomainRejected(SubdomainRejectedException rejection) {
        return switch (rejection.rejection()) {
            case INVALID -> TenancyProblem.SUBDOMAIN_INVALID.toProblem();
            case RESERVED -> TenancyProblem.SUBDOMAIN_RESERVED.toProblem();
        };
    }

    @ExceptionHandler(InvalidTransitionException.class)
    ProblemDetail invalidTransition() {
        return TenancyProblem.INVALID_TRANSITION.toProblem();
    }

    @ExceptionHandler(ProvisioningIncompleteException.class)
    ProblemDetail provisioningIncomplete() {
        return TenancyProblem.PROVISIONING_INCOMPLETE.toProblem();
    }

    @ExceptionHandler(RetentionExpiredException.class)
    ProblemDetail retentionExpired() {
        return TenancyProblem.RETENTION_EXPIRED.toProblem();
    }

    @ExceptionHandler(RetentionInvalidException.class)
    ProblemDetail retentionInvalid() {
        return TenancyProblem.RETENTION_INVALID.toProblem();
    }

    /**
     * Malformed JSON, a body that breaks the contract limits, a missing query parameter, or a parameter checked by
     * the controller ({@link RequestFields}).
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            InvalidRequestException.class})
    ProblemDetail invalidRequest() {
        return TenancyProblem.INVALID_REQUEST.toProblem();
    }
}
