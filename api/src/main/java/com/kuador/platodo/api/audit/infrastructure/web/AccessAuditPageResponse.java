package com.kuador.platodo.api.audit.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog.AuditPage;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;

/** {@code AccessAuditPage} of the contract. */
record AccessAuditPageResponse(List<Item> items, int page, int size, long totalItems) {

    static AccessAuditPageResponse of(AuditPage audit, int page, int size) {
        return new AccessAuditPageResponse(audit.items().stream().map(Item::of).toList(), page, size,
                audit.totalItems());
    }

    /** {@code AccessAuditEvent} of the contract; {@code sessionId} and {@code reason} are null on denials. */
    record Item(UUID id, UUID sessionId, UUID platformUserId, String reason, boolean reasonRedacted,
            String httpMethod, String routeTemplate, String outcome, Instant occurredAt) {

        static Item of(AccessAuditEvent event) {
            return new Item(event.id(), event.sessionId().orElse(null), event.platformUserId(),
                    event.reason().orElse(null), event.reasonRedacted(), event.httpMethod(), event.routeTemplate(),
                    event.outcome().name(), event.occurredAt());
        }
    }
}
