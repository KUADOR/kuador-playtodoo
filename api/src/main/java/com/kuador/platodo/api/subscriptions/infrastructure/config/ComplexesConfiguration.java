package com.kuador.platodo.api.subscriptions.infrastructure.config;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.subscriptions.application.ComplexDetails;
import com.kuador.platodo.api.subscriptions.application.GetComplex;
import com.kuador.platodo.api.subscriptions.application.ListComplexes;
import com.kuador.platodo.api.subscriptions.application.RegisterComplex;
import com.kuador.platodo.api.subscriptions.application.RetryComplexProvisioning;
import com.kuador.platodo.api.subscriptions.application.port.out.LastPayments;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionHistory;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.infrastructure.persistence.global.JpaLastPayments;
import com.kuador.platodo.api.subscriptions.infrastructure.persistence.global.JpaSubscriptions;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantOnboarding;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registration, list and detail of complexes with their subscription (ADR-014, ADR-018), in the global schema. The
 * tenant side goes through the public ports of Tenancy: {@link TenantOnboarding} and {@link TenantDirectory}.
 */
@Configuration(proxyBeanMethods = false)
class ComplexesConfiguration {

    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";
    private static final String GLOBAL_TRANSACTIONS = "global";

    private final EntityManager global;
    private final TransactionTemplate globalTransaction;
    private final Clock clock;

    ComplexesConfiguration(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager globalTx, ObjectProvider<Clock> clock) {
        this.global = SharedEntityManagerCreator.createSharedEntityManager(global);
        this.globalTransaction = new TransactionTemplate(globalTx);
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Bean
    Subscriptions subscriptions() {
        return new JpaSubscriptions(global, JsonMapper.builder().build());
    }

    @Bean
    LastPayments lastPayments() {
        return new JpaLastPayments(global);
    }

    @Bean
    ComplexDetails complexDetails(Subscriptions subscriptions, TenantDirectory directory,
            PlatformUserNames userNames) {
        return new ComplexDetails(subscriptions, directory, userNames, clock);
    }

    @Bean
    RegisterComplex registerComplex(CapabilityGuard guard, Plans plans, Subscriptions subscriptions,
            SubscriptionHistory history, TenantOnboarding onboarding, ComplexDetails details) {
        return new RegisterComplex(guard, plans, subscriptions, history, onboarding, details, globalTransaction,
                clock);
    }

    @Bean
    RetryComplexProvisioning retryComplexProvisioning(CapabilityGuard guard, TenantOnboarding onboarding,
            ComplexDetails details) {
        return new RetryComplexProvisioning(guard, onboarding, details);
    }

    @Bean
    GetComplex getComplex(CapabilityGuard guard, ComplexDetails details) {
        return new GetComplex(guard, details);
    }

    @Bean
    ListComplexes listComplexes(CapabilityGuard guard, Subscriptions subscriptions, TenantDirectory directory,
            LastPayments lastPayments) {
        return new ListComplexes(guard, subscriptions, directory, lastPayments);
    }
}
