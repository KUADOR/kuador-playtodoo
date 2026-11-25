package com.kuador.platodo.api.tenancy.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.kuador.platodo.api.tenancy.application.PurgeDueTenants;

/** Scheduled jobs of Tenancy; {@code tenancy.purge.enabled=false} turns the purge job off (tests do). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("tenancy.purge.enabled")
@EnableScheduling
class TenancySchedulingConfiguration {

    @Bean
    PurgeDueTenantsJob purgeDueTenantsJob(PurgeDueTenants purge) {
        return new PurgeDueTenantsJob(purge);
    }
}
