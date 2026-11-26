package com.kuador.platodo.api.subscriptions.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.kuador.platodo.api.subscriptions.application.ComplexNotFoundException;
import com.kuador.platodo.api.subscriptions.domain.InvalidComplexRegistrationException;
import com.kuador.platodo.api.subscriptions.domain.PlanNotPublishedException;
import com.kuador.platodo.api.tenancy.application.port.in.ProvisioningNotFailedException;
import com.kuador.platodo.api.tenancy.application.port.in.SubdomainUnavailableException;

/**
 * Problem Details of {@link ComplexController}. Guard denials ({@code 401}, {@code 403}) are rendered for every
 * platform operation by Identity and access.
 */
@RestControllerAdvice(assignableTypes = ComplexController.class)
class ComplexesProblemHandler {

    @ExceptionHandler(InvalidComplexRegistrationException.class)
    ProblemDetail validationFailed(InvalidComplexRegistrationException invalid) {
        return SubscriptionsProblem.VALIDATION_FAILED.toProblem(invalid.fieldErrors());
    }

    @ExceptionHandler(SubdomainUnavailableException.class)
    ProblemDetail subdomainUnavailable(SubdomainUnavailableException unavailable) {
        return switch (unavailable.reason()) {
            case INVALID -> SubscriptionsProblem.SUBDOMAIN_INVALID.toProblem();
            case RESERVED -> SubscriptionsProblem.SUBDOMAIN_RESERVED.toProblem();
            case TAKEN -> SubscriptionsProblem.SUBDOMAIN_TAKEN.toProblem();
        };
    }

    @ExceptionHandler(PlanNotPublishedException.class)
    ProblemDetail planNotPublished() {
        return SubscriptionsProblem.PLAN_NOT_PUBLISHED.toProblem();
    }

    @ExceptionHandler(ComplexNotFoundException.class)
    ProblemDetail complexNotFound() {
        return SubscriptionsProblem.COMPLEX_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(ProvisioningNotFailedException.class)
    ProblemDetail notProvisioningFailed() {
        return SubscriptionsProblem.NOT_PROVISIONING_FAILED.toProblem();
    }

    @ExceptionHandler(InvalidSubscriptionsRequestException.class)
    ProblemDetail invalidRequest(InvalidSubscriptionsRequestException invalid) {
        return SubscriptionsProblem.INVALID_REQUEST.toProblem(invalid.getMessage());
    }

    /** Malformed JSON, or a path or query parameter of the wrong type. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malformed() {
        return SubscriptionsProblem.INVALID_REQUEST.toProblem();
    }
}
