package com.kuador.platodo.api.tenancy.infrastructure.web;

/**
 * Tenant resolution or the platform access guard refused a request before its handler ran;
 * {@link TenancyProblemHandler} answers it.
 */
final class TenantRequestRejectedException extends RuntimeException {

    private final TenancyProblem problem;

    TenantRequestRejectedException(TenancyProblem problem) {
        super(problem.name(), null, false, false);
        this.problem = problem;
    }

    TenancyProblem problem() {
        return problem;
    }
}
