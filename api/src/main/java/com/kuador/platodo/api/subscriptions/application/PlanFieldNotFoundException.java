package com.kuador.platodo.api.subscriptions.application;

/** No plan field with this id ({@code 404 PLAN_FIELD_NOT_FOUND}). */
public final class PlanFieldNotFoundException extends RuntimeException {

    public PlanFieldNotFoundException() {
        super("Plan field not found");
    }
}
