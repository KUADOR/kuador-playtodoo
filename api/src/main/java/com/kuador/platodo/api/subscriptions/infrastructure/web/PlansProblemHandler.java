package com.kuador.platodo.api.subscriptions.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.kuador.platodo.api.subscriptions.application.FieldNameTakenException;
import com.kuador.platodo.api.subscriptions.application.PlanFieldNotFoundException;
import com.kuador.platodo.api.subscriptions.application.PlanNameTakenException;
import com.kuador.platodo.api.subscriptions.application.PlanNotFoundException;
import com.kuador.platodo.api.subscriptions.application.StaleVersionException;
import com.kuador.platodo.api.subscriptions.domain.BasicFieldReadOnlyException;
import com.kuador.platodo.api.subscriptions.domain.InvalidPlanFieldsException;
import com.kuador.platodo.api.subscriptions.domain.PlanIncompleteException;

/**
 * Problem Details of {@link PlanController} and {@link PlanFieldController}. Guard denials ({@code 401}, {@code 403})
 * are rendered for every platform operation by Identity and access.
 */
@RestControllerAdvice(assignableTypes = {PlanController.class, PlanFieldController.class})
class PlansProblemHandler {

    @ExceptionHandler(InvalidPlanFieldsException.class)
    ProblemDetail validationFailed(InvalidPlanFieldsException invalid) {
        return PlansProblem.VALIDATION_FAILED.toProblem(invalid.fieldErrors());
    }

    @ExceptionHandler(PlanIncompleteException.class)
    ProblemDetail planIncomplete(PlanIncompleteException incomplete) {
        return PlansProblem.PLAN_INCOMPLETE.toProblem(incomplete.missingFields());
    }

    @ExceptionHandler(BasicFieldReadOnlyException.class)
    ProblemDetail basicFieldReadOnly() {
        return PlansProblem.BASIC_FIELD_READ_ONLY.toProblem();
    }

    @ExceptionHandler(StaleVersionException.class)
    ProblemDetail staleState() {
        return PlansProblem.STALE_STATE.toProblem();
    }

    @ExceptionHandler(PlanNameTakenException.class)
    ProblemDetail planNameTaken() {
        return PlansProblem.PLAN_NAME_TAKEN.toProblem();
    }

    @ExceptionHandler(FieldNameTakenException.class)
    ProblemDetail fieldNameTaken() {
        return PlansProblem.FIELD_NAME_TAKEN.toProblem();
    }

    @ExceptionHandler(PlanNotFoundException.class)
    ProblemDetail planNotFound() {
        return PlansProblem.PLAN_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(PlanFieldNotFoundException.class)
    ProblemDetail planFieldNotFound() {
        return PlansProblem.PLAN_FIELD_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(InvalidPlansRequestException.class)
    ProblemDetail invalidRequest(InvalidPlansRequestException invalid) {
        return PlansProblem.INVALID_REQUEST.toProblem(invalid.getMessage());
    }

    /** Malformed JSON, an unknown enum value, or a path or query parameter of the wrong type. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malformed() {
        return PlansProblem.INVALID_REQUEST.toProblem();
    }
}
