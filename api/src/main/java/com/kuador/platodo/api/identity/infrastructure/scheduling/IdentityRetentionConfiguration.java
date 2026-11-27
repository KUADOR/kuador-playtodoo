package com.kuador.platodo.api.identity.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.kuador.platodo.api.identity.application.DeleteExpiredCapabilityChanges;

/** Scheduled retention of Identity; {@code playtodoo.retention.enabled=false} turns it off (tests do). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("playtodoo.retention.enabled")
@EnableScheduling
class IdentityRetentionConfiguration {

    @Bean
    CapabilityChangeRetentionJob capabilityChangeRetentionJob(DeleteExpiredCapabilityChanges deleteExpired) {
        return new CapabilityChangeRetentionJob(deleteExpired);
    }
}
