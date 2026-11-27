package com.kuador.platodo.api.subscriptions.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.kuador.platodo.api.subscriptions.application.DeleteExpiredSubscriptionHistory;

/** Scheduled retention of Plans and subscriptions; {@code playtodoo.retention.enabled=false} turns it off. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("playtodoo.retention.enabled")
@EnableScheduling
class SubscriptionsRetentionConfiguration {

    @Bean
    SubscriptionHistoryRetentionJob subscriptionHistoryRetentionJob(DeleteExpiredSubscriptionHistory deleteExpired) {
        return new SubscriptionHistoryRetentionJob(deleteExpired);
    }
}
