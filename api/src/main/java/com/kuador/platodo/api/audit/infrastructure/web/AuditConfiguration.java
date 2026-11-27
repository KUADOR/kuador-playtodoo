package com.kuador.platodo.api.audit.infrastructure.web;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.audit.application.AuthorizeSupportAccess;
import com.kuador.platodo.api.audit.application.CloseSupportSession;
import com.kuador.platodo.api.audit.application.CurrentSupportCaller;
import com.kuador.platodo.api.audit.application.DeleteExpiredAccessAudit;
import com.kuador.platodo.api.audit.application.InvalidateAccessSessionsOfPurgingTenant;
import com.kuador.platodo.api.audit.application.ListAuditEntries;
import com.kuador.platodo.api.audit.application.OpenSupportSession;
import com.kuador.platodo.api.audit.application.RedactAccessReasonsOfPurgedTenant;
import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.infrastructure.events.TenantPurgedAuditListener;
import com.kuador.platodo.api.audit.infrastructure.events.TenantPurgingAuditListener;
import com.kuador.platodo.api.audit.infrastructure.persistence.global.JpaAccessAuditLog;
import com.kuador.platodo.api.audit.infrastructure.persistence.global.JpaAccessReasonRedaction;
import com.kuador.platodo.api.audit.infrastructure.persistence.global.JpaAccessSessionInvalidation;
import com.kuador.platodo.api.audit.infrastructure.persistence.global.JpaAccessSessions;
import com.kuador.platodo.api.audit.infrastructure.persistence.global.JpaExpiredAccessAudit;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantProfileLock;
import com.kuador.platodo.api.tenancy.application.port.out.SupportAccessAuthorization;

import jakarta.persistence.EntityManagerFactory;

/**
 * Audit module (PLATODO-26 design, ADR-011 as adjusted by ADR-017 and ADR-020): support sessions and audit trail of
 * the Kuador team in the global schema, the support access operations, the {@link SupportAccessAuthorization} that
 * Tenancy's guard asks, the end of the open sessions when a complex starts its purge (ADR-015), the anonymization of
 * the reasons when it is purged (ADR-013) and the deletion of the audit past its retention (ADR-016).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccessSessionProperties.class)
class AuditConfiguration {

    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";
    private static final String GLOBAL_TRANSACTIONS = "global";
    private static final String RETENTION_DAYS = "${playtodoo.audit.retention-days}";

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
    CurrentSupportCaller currentSupportCaller(CapabilityGuard guard) {
        return new CurrentSupportCaller(guard);
    }

    @Bean
    OpenSupportSession openSupportSession(CurrentSupportCaller callers, TenantDirectory tenants,
            AccessSessions sessions, AccessAuditLog auditLog,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager global, AccessSessionProperties properties) {
        return new OpenSupportSession(
                new OpenSupportSession.Ports(callers, tenants, sessions, auditLog, new TransactionTemplate(global)),
                properties.timeToLive(), clock);
    }

    @Bean
    CloseSupportSession closeSupportSession(CapabilityGuard guard, AccessSessions sessions, AccessAuditLog auditLog,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager global) {
        return new CloseSupportSession(
                new CloseSupportSession.Ports(guard, sessions, auditLog, new TransactionTemplate(global)), clock);
    }

    /** @param retentionDays how long the audit is kept (ADR-016: 365 by decision of Kuador) */
    @Bean
    ListAuditEntries listAuditEntries(CapabilityGuard guard, TenantDirectory tenants, AccessAuditLog auditLog,
            PlatformUserNames userNames, @Value(RETENTION_DAYS) int retentionDays) {
        return new ListAuditEntries(guard, tenants, auditLog, userNames, clock, Duration.ofDays(retentionDays));
    }

    /** RecordAccess runs in a transaction of its own (D6), committed before the audited request runs. */
    @Bean
    SupportAccessAuthorization supportAccessAuthorization(CurrentSupportCaller callers, TenantProfileLock tenants,
            AccessSessions sessions, AccessAuditLog auditLog,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager global) {
        TransactionTemplate ownTransaction = new TransactionTemplate(global);
        ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new AuthorizeSupportAccess(
                new AuthorizeSupportAccess.Ports(callers, tenants, sessions, auditLog, ownTransaction), clock);
    }

    @Bean
    TenantPurgedAuditListener tenantPurgedAuditListener(
            @Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global) {
        return new TenantPurgedAuditListener(new RedactAccessReasonsOfPurgedTenant(
                new JpaAccessReasonRedaction(SharedEntityManagerCreator.createSharedEntityManager(global))));
    }

    @Bean
    TenantPurgingAuditListener tenantPurgingAuditListener(
            @Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global) {
        return new TenantPurgingAuditListener(new InvalidateAccessSessionsOfPurgingTenant(
                new JpaAccessSessionInvalidation(SharedEntityManagerCreator.createSharedEntityManager(global))));
    }

    /** @param retentionDays how long the audit is kept (ADR-016: 365 by decision of Kuador) */
    @Bean
    DeleteExpiredAccessAudit deleteExpiredAccessAudit(
            @Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global,
            @Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager globalTx,
            @Value(RETENTION_DAYS) int retentionDays) {
        return new DeleteExpiredAccessAudit(
                new JpaExpiredAccessAudit(SharedEntityManagerCreator.createSharedEntityManager(global)),
                new TransactionTemplate(globalTx), clock, Duration.ofDays(retentionDays));
    }
}
