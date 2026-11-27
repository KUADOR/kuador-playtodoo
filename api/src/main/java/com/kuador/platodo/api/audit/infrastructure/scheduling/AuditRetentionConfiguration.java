package com.kuador.platodo.api.audit.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.kuador.platodo.api.audit.application.DeleteExpiredAccessAudit;

/** Scheduled retention of Audit; {@code playtodoo.retention.enabled=false} turns it off (tests do). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("playtodoo.retention.enabled")
@EnableScheduling
class AuditRetentionConfiguration {

    @Bean
    AccessAuditRetentionJob accessAuditRetentionJob(DeleteExpiredAccessAudit deleteExpired) {
        return new AccessAuditRetentionJob(deleteExpired);
    }
}
