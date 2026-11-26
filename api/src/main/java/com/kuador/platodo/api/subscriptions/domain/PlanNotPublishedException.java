package com.kuador.platodo.api.subscriptions.domain;

/** The plan to assign does not exist or is a draft ({@code 422 PLAN_NOT_PUBLISHED}). */
public final class PlanNotPublishedException extends RuntimeException {

    public PlanNotPublishedException() {
        super("The plan does not exist or is not published", null, false, false);
    }
}
