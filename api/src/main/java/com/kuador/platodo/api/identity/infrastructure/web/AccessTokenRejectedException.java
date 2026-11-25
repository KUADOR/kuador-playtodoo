package com.kuador.platodo.api.identity.infrastructure.web;

/** {@link AccessTokenFilter} refused the token of a request before any handler ran. */
final class AccessTokenRejectedException extends RuntimeException {

    private final IdentityProblem problem;

    AccessTokenRejectedException(IdentityProblem problem) {
        super(problem.name(), null, false, false);
        this.problem = problem;
    }

    IdentityProblem problem() {
        return problem;
    }
}
