package com.kuador.platodo.api.subscriptions.infrastructure.config;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.subscriptions.application.ChangeComplexPlan;
import com.kuador.platodo.api.subscriptions.application.ChangeSubscriptionStatus;
import com.kuador.platodo.api.subscriptions.application.ComplexDetails;
import com.kuador.platodo.api.subscriptions.application.ComplexLocks;
import com.kuador.platodo.api.subscriptions.application.ListComplexHistory;
import com.kuador.platodo.api.subscriptions.application.ListComplexPayments;
import com.kuador.platodo.api.subscriptions.application.RecordComplexPayment;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionEvents;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionHistory;
import com.kuador.platodo.api.subscriptions.application.port.out.SubscriptionPayments;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.infrastructure.events.SpringSubscriptionEvents;
import com.kuador.platodo.api.subscriptions.infrastructure.persistence.global.JpaSubscriptionHistory;
import com.kuador.platodo.api.subscriptions.infrastructure.persistence.global.JpaSubscriptionPayments;
import com.kuador.platodo.api.tenancy.application.port.in.TenantProfileLock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Plan changes, manual status changes, payments and history of a complex (ADR-014, ADR-018), in the global schema.
 * The tenant row is locked through the public port {@link TenantProfileLock} of Tenancy.
 */
@Configuration(proxyBeanMethods = false)
class SubscriptionChangesConfiguration {

    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";
    private static final String GLOBAL_TRANSACTIONS = "global";

    private final EntityManager global;
    private final TransactionTemplate globalTransaction;
    private final Clock clock;

    SubscriptionChangesConfiguration(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager globalTx, ObjectProvider<Clock> clock) {
        this.global = SharedEntityManagerCreator.createSharedEntityManager(global);
        this.globalTransaction = new TransactionTemplate(globalTx);
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Bean
    SubscriptionHistory subscriptionHistory() {
        return new JpaSubscriptionHistory(global);
    }

    @Bean
    SubscriptionPayments subscriptionPayments() {
        return new JpaSubscriptionPayments(global);
    }

    @Bean
    SubscriptionEvents subscriptionEvents(ApplicationEventPublisher publisher) {
        return new SpringSubscriptionEvents(publisher);
    }

    @Bean
    ComplexLocks complexLocks(Subscriptions subscriptions, TenantProfileLock tenants) {
        return new ComplexLocks(subscriptions, tenants);
    }

    @Bean
    ChangeComplexPlan changeComplexPlan(CapabilityGuard guard, ComplexLocks locks, Plans plans,
            Subscriptions subscriptions, SubscriptionHistory history, SubscriptionEvents events,
            ComplexDetails details) {
        return new ChangeComplexPlan(guard, locks, plans, subscriptions, history, events, details, globalTransaction,
                clock);
    }

    @Bean
    ChangeSubscriptionStatus changeSubscriptionStatus(CapabilityGuard guard, ComplexLocks locks,
            Subscriptions subscriptions, SubscriptionHistory history, SubscriptionEvents events,
            ComplexDetails details) {
        return new ChangeSubscriptionStatus(guard, locks, subscriptions, history, events, details, globalTransaction,
                clock);
    }

    @Bean
    RecordComplexPayment recordComplexPayment(CapabilityGuard guard, Subscriptions subscriptions,
            SubscriptionPayments payments, PlatformUserNames userNames) {
        return new RecordComplexPayment(guard, subscriptions, payments, userNames, globalTransaction, clock);
    }

    @Bean
    ListComplexPayments listComplexPayments(CapabilityGuard guard, ComplexDetails details,
            SubscriptionPayments payments, PlatformUserNames userNames) {
        return new ListComplexPayments(guard, details, payments, userNames);
    }

    @Bean
    ListComplexHistory listComplexHistory(CapabilityGuard guard, ComplexDetails details,
            SubscriptionHistory history, PlatformUserNames userNames) {
        return new ListComplexHistory(guard, details, history, userNames, clock);
    }
}
