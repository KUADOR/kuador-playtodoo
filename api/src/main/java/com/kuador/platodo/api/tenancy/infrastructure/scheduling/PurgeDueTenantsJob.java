package com.kuador.platodo.api.tenancy.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.kuador.platodo.api.tenancy.application.PurgeDueTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Daily permanent deletion of the suspended and archived complexes whose retention ended (ADR-013, ADR-015), at
 * {@code tenancy.purge.cron} in UTC.
 * Safe on several api instances at once: see {@link PurgeDueTenants}.
 */
public class PurgeDueTenantsJob {

    private static final Logger log = LoggerFactory.getLogger(PurgeDueTenantsJob.class);

    private final PurgeDueTenants purge;

    public PurgeDueTenantsJob(PurgeDueTenants purge) {
        this.purge = purge;
    }

    @Scheduled(cron = "${tenancy.purge.cron}", zone = "UTC")
    public void run() {
        int claimed = purge.claimDue();
        log.info("Claimed {} complexes for their permanent deletion", claimed);
        purge.pending().forEach(this::purgeOne);
    }

    /** A failure in one complex leaves it PURGING for the next run; it must not stop the others. */
    private void purgeOne(TenantId tenant) {
        try {
            if (purge.purge(tenant)) {
                log.info("Complex {} permanently deleted", tenant.value());
            }
        } catch (RuntimeException error) {
            log.error("Could not purge complex {}; it stays PURGING and is retried on the next run",
                    tenant.value(), error);
        }
    }
}
