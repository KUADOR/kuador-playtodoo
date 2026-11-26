package com.kuador.platodo.api.subscriptions.infrastructure.web;

/** Paths of the tags «Plans» and «Plan fields» of {@code contracts/platform-backoffice.openapi.yaml}. */
final class PlansApiPaths {

    static final String PLATFORM = "/api/v1/platform";
    static final String PLANS = "/plans";
    static final String PLAN = PLANS + "/{planId}";
    static final String PLAN_PUBLICATION = PLAN + "/publication";
    static final String PLAN_OPTIONS = "/plan-options";
    static final String PLAN_FIELDS = "/plan-fields";
    static final String PLAN_FIELD = PLAN_FIELDS + "/{fieldId}";
    static final String PLAN_FIELD_RETIREMENT = PLAN_FIELD + "/retirement";

    private PlansApiPaths() {
    }
}
