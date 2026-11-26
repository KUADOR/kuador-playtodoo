package com.kuador.platodo.api.subscriptions.application;

/** No plan with this id, or a draft asked for by someone who is not the SUPER_ADMIN ({@code 404 PLAN_NOT_FOUND}). */
public final class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException() {
        super("Plan not found");
    }
}
