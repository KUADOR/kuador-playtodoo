package com.kuador.platodo.api.subscriptions.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.kuador.platodo.api.subscriptions.application.ComplexNotFoundException;
import com.kuador.platodo.api.subscriptions.application.RetentionExpiredException;
import com.kuador.platodo.api.subscriptions.application.StaleVersionException;
import com.kuador.platodo.api.subscriptions.domain.InvalidPaymentException;
import com.kuador.platodo.api.subscriptions.domain.PlanNotPublishedException;
import com.kuador.platodo.api.subscriptions.domain.SamePlanException;
import com.kuador.platodo.api.subscriptions.domain.SubscriptionCancelledException;
import com.kuador.platodo.api.subscriptions.domain.TransitionNotAllowedException;
import com.kuador.platodo.api.tenancy.domain.NotInRetentionException;
import com.kuador.platodo.api.tenancy.domain.RetentionInvalidException;

/**
 * Problem Details of {@link SubscriptionController}. Guard denials ({@code 401}, {@code 403}) are rendered for every
 * platform operation by Identity and access.
 */
@RestControllerAdvice(assignableTypes = SubscriptionController.class)
class SubscriptionProblemHandler {

    @ExceptionHandler(ComplexNotFoundException.class)
    ProblemDetail complexNotFound() {
        return SubscriptionsProblem.COMPLEX_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(StaleVersionException.class)
    ProblemDetail staleState() {
        return SubscriptionsProblem.STALE_STATE.toProblem();
    }

    @ExceptionHandler(PlanNotPublishedException.class)
    ProblemDetail planNotPublished() {
        return SubscriptionsProblem.PLAN_NOT_PUBLISHED.toProblem();
    }

    @ExceptionHandler(SamePlanException.class)
    ProblemDetail samePlan() {
        return SubscriptionsProblem.SAME_PLAN.toProblem();
    }

    @ExceptionHandler(SubscriptionCancelledException.class)
    ProblemDetail subscriptionCancelled() {
        return SubscriptionsProblem.SUBSCRIPTION_CANCELLED.toProblem();
    }

    @ExceptionHandler(TransitionNotAllowedException.class)
    ProblemDetail transitionNotAllowed(TransitionNotAllowedException notAllowed) {
        return SubscriptionsProblem.TRANSITION_NOT_ALLOWED.toProblem(notAllowed.getMessage() + ".");
    }

    @ExceptionHandler(RetentionExpiredException.class)
    ProblemDetail retentionExpired() {
        return SubscriptionsProblem.RETENTION_EXPIRED.toProblem();
    }

    @ExceptionHandler(NotInRetentionException.class)
    ProblemDetail notInRetention() {
        return SubscriptionsProblem.NOT_IN_RETENTION.toProblem();
    }

    @ExceptionHandler(RetentionInvalidException.class)
    ProblemDetail retentionDeadlineInPast() {
        return SubscriptionsProblem.RETENTION_DEADLINE_IN_PAST.toProblem();
    }

    @ExceptionHandler(InvalidRetentionRequestException.class)
    ProblemDetail invalidRetention(InvalidRetentionRequestException invalid) {
        return SubscriptionsProblem.VALIDATION_FAILED.toProblem(invalid.fieldErrors());
    }

    @ExceptionHandler(InvalidPaymentException.class)
    ProblemDetail validationFailed(InvalidPaymentException invalid) {
        return SubscriptionsProblem.VALIDATION_FAILED.toProblem(invalid.fieldErrors());
    }

    @ExceptionHandler(InvalidSubscriptionsRequestException.class)
    ProblemDetail invalidRequest(InvalidSubscriptionsRequestException invalid) {
        return SubscriptionsProblem.INVALID_REQUEST.toProblem(invalid.getMessage());
    }

    /** Malformed JSON, an unknown status, a date that is not a date, or a path parameter of the wrong type. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malformed() {
        return SubscriptionsProblem.INVALID_REQUEST.toProblem();
    }
}
