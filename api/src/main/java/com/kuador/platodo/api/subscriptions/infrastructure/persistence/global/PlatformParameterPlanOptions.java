package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.util.List;

import com.kuador.platodo.api.subscriptions.application.port.out.PlanOptionsSource;
import com.kuador.platodo.api.subscriptions.domain.PlanOptions;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan options from {@code global.platform_parameter} (migration V7): the JSON lists {@value #CURRENCIES} and
 * {@value #PERIODICITIES}. Read on every call, so a change of the parameters applies to the next request. A missing
 * parameter means no allowed values.
 */
public final class PlatformParameterPlanOptions implements PlanOptionsSource {

    static final String CURRENCIES = "plan.currencies";
    static final String PERIODICITIES = "plan.periodicities";

    private final EntityManager global;
    private final JsonMapper json;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public PlatformParameterPlanOptions(EntityManager global, JsonMapper json) {
        this.global = global;
        this.json = json;
    }

    @Override
    public PlanOptions current() {
        return new PlanOptions(listOf(CURRENCIES), listOf(PERIODICITIES));
    }

    private List<String> listOf(String key) {
        PlanOptionParameterEntity parameter = global.find(PlanOptionParameterEntity.class, key);
        if (parameter == null) {
            return List.of();
        }
        return List.of(json.readValue(parameter.value(), String[].class));
    }
}
