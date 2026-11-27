package com.kuador.platodo.api.subscriptions.domain;

/** The plan to assign is already the plan of the complex ({@code 422 SAME_PLAN}). */
public final class SamePlanException extends RuntimeException {

    public SamePlanException() {
        super("The complex already has this plan", null, false, false);
    }
}
