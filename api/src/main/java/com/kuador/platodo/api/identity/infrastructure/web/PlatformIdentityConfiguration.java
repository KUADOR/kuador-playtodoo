package com.kuador.platodo.api.identity.infrastructure.web;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.identity.application.ChangeOperatorCapabilities;
import com.kuador.platodo.api.identity.application.DeleteExpiredCapabilityChanges;
import com.kuador.platodo.api.identity.application.GetCurrentPlatformUser;
import com.kuador.platodo.api.identity.application.ListCapabilityChanges;
import com.kuador.platodo.api.identity.application.ListOperatorCapabilities;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog;
import com.kuador.platodo.api.identity.infrastructure.events.SpringOperatorCapabilityEvents;
import com.kuador.platodo.api.identity.infrastructure.persistence.global.JpaCapabilityChangeLog;
import com.kuador.platodo.api.identity.infrastructure.persistence.global.JpaExpiredCapabilityChanges;
import com.kuador.platodo.api.identity.infrastructure.persistence.global.JpaOperatorCapabilities;
import com.kuador.platodo.api.identity.infrastructure.persistence.global.JpaPlatformUserProfiles;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Platform roles and operator capabilities (ADR-017): the {@link CapabilityGuard} every platform operation uses and
 * the operations of the tags «Me» and «Operator capabilities».
 */
@Configuration(proxyBeanMethods = false)
class PlatformIdentityConfiguration {

    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";
    private static final String GLOBAL_TRANSACTIONS = "global";

    private final EntityManager global;
    private final Clock clock;
    private final Duration changesRetention;

    /** @param changesRetentionDays how long capability changes are kept (ADR-016: 365 by decision of Kuador) */
    PlatformIdentityConfiguration(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global,
            ObjectProvider<Clock> clock, @Value("${playtodoo.history.retention-days}") int changesRetentionDays) {
        this.global = SharedEntityManagerCreator.createSharedEntityManager(global);
        this.clock = clock.getIfAvailable(Clock::systemUTC);
        this.changesRetention = Duration.ofDays(changesRetentionDays);
    }

    @Bean
    JpaOperatorCapabilities operatorCapabilities() {
        return new JpaOperatorCapabilities(global);
    }

    @Bean
    CapabilityChangeLog capabilityChangeLog() {
        return new JpaCapabilityChangeLog(global);
    }

    @Bean
    CapabilityGuard capabilityGuard(JpaOperatorCapabilities operatorCapabilities) {
        return new CapabilityGuard(operatorCapabilities);
    }

    /** Also the public {@code PlatformUserNames} port of the module. */
    @Bean
    JpaPlatformUserProfiles platformUserProfiles() {
        return new JpaPlatformUserProfiles(global);
    }

    @Bean
    GetCurrentPlatformUser getCurrentPlatformUser(CapabilityGuard guard, JpaPlatformUserProfiles profiles) {
        return new GetCurrentPlatformUser(guard, profiles);
    }

    @Bean
    ListOperatorCapabilities listOperatorCapabilities(CapabilityGuard guard,
            JpaOperatorCapabilities operatorCapabilities) {
        return new ListOperatorCapabilities(guard, operatorCapabilities);
    }

    @Bean
    ChangeOperatorCapabilities changeOperatorCapabilities(CapabilityGuard guard,
            JpaOperatorCapabilities operatorCapabilities, CapabilityChangeLog changeLog,
            ApplicationEventPublisher publisher, @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager globalTx) {
        return new ChangeOperatorCapabilities(guard, operatorCapabilities, changeLog,
                new SpringOperatorCapabilityEvents(publisher), new TransactionTemplate(globalTx), clock);
    }

    @Bean
    ListCapabilityChanges listCapabilityChanges(CapabilityGuard guard, CapabilityChangeLog changeLog) {
        return new ListCapabilityChanges(guard, changeLog, clock, changesRetention);
    }

    @Bean
    DeleteExpiredCapabilityChanges deleteExpiredCapabilityChanges(
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager globalTx) {
        return new DeleteExpiredCapabilityChanges(new JpaExpiredCapabilityChanges(global),
                new TransactionTemplate(globalTx), clock, changesRetention);
    }
}
