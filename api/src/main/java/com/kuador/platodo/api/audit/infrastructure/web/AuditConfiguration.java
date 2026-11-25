package com.kuador.platodo.api.audit.infrastructure.web;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.audit.application.AuthorizePlatformAccess;
import com.kuador.platodo.api.audit.application.CloseAccessSession;
import com.kuador.platodo.api.audit.application.ListAccessAudit;
import com.kuador.platodo.api.audit.application.OpenAccessSession;
import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.application.port.out.CurrentPlatformOwner;
import com.kuador.platodo.api.audit.infrastructure.identity.PrincipalCurrentPlatformOwner;
import com.kuador.platodo.api.audit.infrastructure.persistence.global.JpaAccessAuditLog;
import com.kuador.platodo.api.audit.infrastructure.persistence.global.JpaAccessSessions;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.out.PlatformAccessAuthorization;

import jakarta.persistence.EntityManagerFactory;

/**
 * Audit module (PLATODO-26 design, ADR-011): access sessions and audit trail of the Kuador owner in the global schema,
 * the platform access operations and the {@link PlatformAccessAuthorization} that Tenancy's guard asks.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccessSessionProperties.class)
class AuditConfiguration {

    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";
    private static final String GLOBAL_TRANSACTIONS = "global";

    private final Clock clock;

    AuditConfiguration(ObjectProvider<Clock> clock) {
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Bean
    AccessSessions accessSessions(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global) {
        return new JpaAccessSessions(SharedEntityManagerCreator.createSharedEntityManager(global));
    }

    @Bean
    AccessAuditLog accessAuditLog(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global) {
        return new JpaAccessAuditLog(SharedEntityManagerCreator.createSharedEntityManager(global));
    }

    @Bean
    CurrentPlatformOwner currentPlatformOwner() {
        return new PrincipalCurrentPlatformOwner();
    }

    @Bean
    OpenAccessSession openAccessSession(CurrentPlatformOwner owner, TenantDirectory tenants,
            AccessSessions sessions, @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager global,
            AccessSessionProperties properties) {
        return new OpenAccessSession(owner, tenants, sessions, new TransactionTemplate(global),
                properties.timeToLive(), clock);
    }

    @Bean
    CloseAccessSession closeAccessSession(CurrentPlatformOwner owner, AccessSessions sessions,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager global) {
        return new CloseAccessSession(owner, sessions, new TransactionTemplate(global), clock);
    }

    @Bean
    ListAccessAudit listAccessAudit(CurrentPlatformOwner owner, TenantDirectory tenants, AccessAuditLog auditLog) {
        return new ListAccessAudit(owner, tenants, auditLog);
    }

    /** RecordAccess runs in a transaction of its own (D6), committed before the audited request runs. */
    @Bean
    PlatformAccessAuthorization platformAccessAuthorization(AccessSessions sessions, AccessAuditLog auditLog,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager global) {
        TransactionTemplate ownTransaction = new TransactionTemplate(global);
        ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new AuthorizePlatformAccess(sessions, auditLog, ownTransaction, clock);
    }
}
