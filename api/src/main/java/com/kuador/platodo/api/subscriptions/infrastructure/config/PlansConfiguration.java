package com.kuador.platodo.api.subscriptions.infrastructure.config;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.subscriptions.application.CreatePlan;
import com.kuador.platodo.api.subscriptions.application.CreatePlanField;
import com.kuador.platodo.api.subscriptions.application.GetPlan;
import com.kuador.platodo.api.subscriptions.application.GetPlanOptions;
import com.kuador.platodo.api.subscriptions.application.ListPlanFields;
import com.kuador.platodo.api.subscriptions.application.ListPlans;
import com.kuador.platodo.api.subscriptions.application.PublishPlan;
import com.kuador.platodo.api.subscriptions.application.RetirePlanField;
import com.kuador.platodo.api.subscriptions.application.UpdatePlan;
import com.kuador.platodo.api.subscriptions.application.UpdatePlanField;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.application.port.out.PlanOptionsSource;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.infrastructure.persistence.global.JpaPlanFieldDefinitions;
import com.kuador.platodo.api.subscriptions.infrastructure.persistence.global.JpaPlans;
import com.kuador.platodo.api.subscriptions.infrastructure.persistence.global.PlatformParameterPlanOptions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import tools.jackson.databind.json.JsonMapper;

/** Plans and plan fields of Plans and subscriptions (ADR-018), all in the global schema. */
@Configuration(proxyBeanMethods = false)
class PlansConfiguration {

    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";
    private static final String GLOBAL_TRANSACTIONS = "global";

    private final EntityManager global;
    private final TransactionOperations globalTransaction;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Clock clock;

    PlansConfiguration(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager globalTx, ObjectProvider<Clock> clock) {
        this.global = SharedEntityManagerCreator.createSharedEntityManager(global);
        this.globalTransaction = new TransactionTemplate(globalTx);
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Bean
    Plans plans() {
        return new JpaPlans(global, json);
    }

    @Bean
    PlanFieldDefinitions planFieldDefinitions() {
        return new JpaPlanFieldDefinitions(global);
    }

    @Bean
    PlanOptionsSource planOptionsSource() {
        return new PlatformParameterPlanOptions(global, json);
    }

    @Bean
    ListPlans listPlans(CapabilityGuard guard, Plans plans) {
        return new ListPlans(guard, plans);
    }

    @Bean
    GetPlan getPlan(CapabilityGuard guard, Plans plans) {
        return new GetPlan(guard, plans);
    }

    @Bean
    CreatePlan createPlan(CapabilityGuard guard, Plans plans, PlanFieldDefinitions fields,
            PlanOptionsSource options) {
        return new CreatePlan(guard, plans, fields, options, globalTransaction);
    }

    @Bean
    UpdatePlan updatePlan(CapabilityGuard guard, Plans plans, PlanFieldDefinitions fields,
            PlanOptionsSource options) {
        return new UpdatePlan(guard, plans, fields, options, globalTransaction);
    }

    @Bean
    PublishPlan publishPlan(CapabilityGuard guard, Plans plans, PlanFieldDefinitions fields) {
        return new PublishPlan(guard, plans, fields, globalTransaction);
    }

    @Bean
    GetPlanOptions getPlanOptions(CapabilityGuard guard, PlanOptionsSource options) {
        return new GetPlanOptions(guard, options);
    }

    @Bean
    ListPlanFields listPlanFields(CapabilityGuard guard, PlanFieldDefinitions fields) {
        return new ListPlanFields(guard, fields);
    }

    @Bean
    CreatePlanField createPlanField(CapabilityGuard guard, PlanFieldDefinitions fields) {
        return new CreatePlanField(guard, fields, globalTransaction);
    }

    @Bean
    UpdatePlanField updatePlanField(CapabilityGuard guard, PlanFieldDefinitions fields) {
        return new UpdatePlanField(guard, fields, globalTransaction);
    }

    @Bean
    RetirePlanField retirePlanField(CapabilityGuard guard, PlanFieldDefinitions fields) {
        return new RetirePlanField(guard, fields, globalTransaction, clock);
    }
}
