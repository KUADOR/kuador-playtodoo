package com.kuador.platodo.api.notifications.infrastructure.persistence.global;

import java.util.LinkedHashMap;
import java.util.Map;

import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerNotifier;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Queues the permanent deletion e-mail to the owner of a complex in {@code global.notification_outbox} (ADR-013, D8),
 * inside the purge transaction. The provider and the template arrive with the notifications epic, which sends and
 * deletes the row.
 */
public final class OutboxTenantOwnerNotifier implements TenantOwnerNotifier {

    /** Outbox type of the e-mail that announces the permanent deletion of a complex to its owner. */
    public static final String TENANT_PERMANENTLY_DELETED = "TENANT_PERMANENTLY_DELETED";

    private final EntityManager global;
    private final JsonMapper json;

    /** {@code global} must be an entity manager of the global persistence unit. */
    public OutboxTenantOwnerNotifier(EntityManager global, JsonMapper json) {
        this.global = global;
        this.json = json;
    }

    /** Flushes at once, so a failing insert fails the purge before the schema is dropped. */
    @Override
    public void notifyPermanentDeletion(PermanentDeletionNotice notice) {
        global.persist(NotificationOutboxEntity.pending(
                TENANT_PERMANENTLY_DELETED, notice.ownerEmail(), payloadOf(notice), notice.purgedAt()));
        global.flush();
    }

    private String payloadOf(PermanentDeletionNotice notice) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("tenantId", notice.tenant().value());
        payload.put("displayName", notice.displayName());
        payload.put("purgedAt", notice.purgedAt().toString());
        return json.writeValueAsString(payload);
    }
}
