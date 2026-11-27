package com.kuador.platodo.api.identity.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.kuador.platodo.api.identity.application.DeleteExpiredCapabilityChanges;

/**
 * Daily deletion of the capability changes past their retention (ADR-016), at {@code playtodoo.retention.cron} in
 * UTC. Safe on several api instances at once: deleting what is already deleted changes nothing.
 */
public class CapabilityChangeRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(CapabilityChangeRetentionJob.class);

    private final DeleteExpiredCapabilityChanges deleteExpired;

    public CapabilityChangeRetentionJob(DeleteExpiredCapabilityChanges deleteExpired) {
        this.deleteExpired = deleteExpired;
    }

    @Scheduled(cron = "${playtodoo.retention.cron}", zone = "UTC")
    public void run() {
        log.info("Deleted {} capability changes past their retention", deleteExpired.run());
    }
}
