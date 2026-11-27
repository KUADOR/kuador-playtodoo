package com.kuador.platodo.api.subscriptions.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.kuador.platodo.api.subscriptions.application.DeleteExpiredSubscriptionHistory;

/**
 * Daily deletion of the subscription history past its retention (ADR-016), at {@code playtodoo.retention.cron} in
 * UTC. Safe on several api instances at once: deleting what is already deleted changes nothing.
 */
public class SubscriptionHistoryRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionHistoryRetentionJob.class);

    private final DeleteExpiredSubscriptionHistory deleteExpired;

    public SubscriptionHistoryRetentionJob(DeleteExpiredSubscriptionHistory deleteExpired) {
        this.deleteExpired = deleteExpired;
    }

    @Scheduled(cron = "${playtodoo.retention.cron}", zone = "UTC")
    public void run() {
        log.info("Deleted {} subscription history entries past their retention", deleteExpired.run());
    }
}
