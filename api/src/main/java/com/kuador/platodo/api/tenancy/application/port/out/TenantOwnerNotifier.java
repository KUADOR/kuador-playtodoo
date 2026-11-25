package com.kuador.platodo.api.tenancy.application.port.out;

import java.time.Instant;
import java.util.Objects;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Tells the owner of a complex about its permanent deletion (ADR-013, D8). Implemented by Notifications, which queues
 * the e-mail in its outbox inside the surrounding {@link GlobalTransaction}: it is sent only if the purge commits.
 */
@FunctionalInterface
public interface TenantOwnerNotifier {

    void notifyPermanentDeletion(PermanentDeletionNotice notice);

    /**
     * @param ownerEmail  recipient; personal data, so it is never logged nor kept after sending
     * @param displayName commercial name of the complex
     */
    record PermanentDeletionNotice(TenantId tenant, String displayName, String ownerEmail, Instant purgedAt) {

        public PermanentDeletionNotice {
            Objects.requireNonNull(tenant, "tenant");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(ownerEmail, "ownerEmail");
            Objects.requireNonNull(purgedAt, "purgedAt");
        }
    }
}
