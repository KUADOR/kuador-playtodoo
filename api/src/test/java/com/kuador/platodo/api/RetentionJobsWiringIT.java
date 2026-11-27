package com.kuador.platodo.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import com.kuador.platodo.api.audit.infrastructure.scheduling.AccessAuditRetentionJob;
import com.kuador.platodo.api.identity.infrastructure.scheduling.CapabilityChangeRetentionJob;
import com.kuador.platodo.api.subscriptions.infrastructure.scheduling.SubscriptionHistoryRetentionJob;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.tenancy.infrastructure.scheduling.PurgeDueTenantsJob;

/**
 * With the jobs turned on, as in every deployed environment, the purge and the three retention jobs are registered.
 * The Spring cron {@code -} keeps them from firing during the test.
 */
@SpringBootTest(properties = {
        "tenancy.purge.enabled=true", "tenancy.purge.cron=-",
        "playtodoo.retention.enabled=true", "playtodoo.retention.cron=-"})
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class RetentionJobsWiringIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void thePurgeAndTheRetentionJobsAreRegisteredWhenEnabled() {
        assertThat(context.getBeanNamesForType(PurgeDueTenantsJob.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(AccessAuditRetentionJob.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(SubscriptionHistoryRetentionJob.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(CapabilityChangeRetentionJob.class)).hasSize(1);
    }
}
