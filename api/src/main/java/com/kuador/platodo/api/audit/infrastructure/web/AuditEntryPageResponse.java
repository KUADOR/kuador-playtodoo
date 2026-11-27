package com.kuador.platodo.api.audit.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.ListAuditEntries.AuditEntries;
import com.kuador.platodo.api.audit.application.ListAuditEntries.AuditEntry;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.audit.domain.AccessOutcome;
import com.kuador.platodo.api.audit.domain.DenialCode;

/** {@code AuditEntryPage} of the contract. */
record AuditEntryPageResponse(List<Item> items, int page, int size, long totalItems) {

    static AuditEntryPageResponse of(AuditEntries entries, int page, int size) {
        return new AuditEntryPageResponse(entries.items().stream().map(Item::of).toList(), page, size,
                entries.totalItems());
    }

    /** {@code UserRef} of the contract. */
    record Actor(UUID id, String displayName) {
    }

    /**
     * {@code AuditEntry} of the contract: {@code sessionId} is null on denials, {@code reason} on denials and once
     * redacted, {@code operationId} for routes that are not contract operations and {@code denialCode} when allowed.
     */
    record Item(UUID id, UUID sessionId, Actor actor, String reason, boolean reasonRedacted, String operationId,
            String httpMethod, String pathTemplate, AccessOutcome result, DenialCode denialCode, Instant occurredAt) {

        static Item of(AuditEntry entry) {
            AccessAuditEvent event = entry.event();
            return new Item(event.id(), event.sessionId().orElse(null),
                    new Actor(event.platformUserId(), entry.actorName()),
                    event.reasonRedacted() ? null : event.reason().orElse(null), event.reasonRedacted(),
                    event.request().operationId().orElse(null), event.request().httpMethod(),
                    event.request().routeTemplate(), event.outcome(), event.denialCode().orElse(null),
                    event.occurredAt());
        }
    }
}
