package com.kuador.platodo.api.subscriptions.application;

/** Another plan has this name, without regard to case ({@code 409 PLAN_NAME_TAKEN}). */
public final class PlanNameTakenException extends RuntimeException {

    public PlanNameTakenException() {
        super("Plan name taken");
    }
}
