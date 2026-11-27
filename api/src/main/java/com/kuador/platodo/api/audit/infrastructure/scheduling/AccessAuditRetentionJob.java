package com.kuador.platodo.api.audit.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.kuador.platodo.api.audit.application.DeleteExpiredAccessAudit;

/**
 * Daily deletion of the access audit past its retention (ADR-016), at {@code playtodoo.retention.cron} in UTC. Safe
 * on several api instances at once: deleting what is already deleted changes nothing.
 */
public class AccessAuditRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AccessAuditRetentionJob.class);

    private final DeleteExpiredAccessAudit deleteExpired;

    public AccessAuditRetentionJob(DeleteExpiredAccessAudit deleteExpired) {
        this.deleteExpired = deleteExpired;
    }

    @Scheduled(cron = "${playtodoo.retention.cron}", zone = "UTC")
    public void run() {
        log.info("Deleted {} access audit rows past their retention", deleteExpired.run());
    }
}
