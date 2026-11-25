package com.kuador.platodo.api.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * One audited request of a Kuador platform user on the host of a complex (ADR-011, D12). It keeps the route template,
 * never the concrete path nor the query string. A denied request carries no session nor reason: the session, if any,
 * was not valid for this complex and its reason belongs to another one.
 *
 * @param reasonRedacted whether the reason was anonymized by the purge of the complex
 */
public record AccessAuditEvent(UUID id, Optional<UUID> sessionId, UUID platformUserId, TenantId tenant,
        Optional<String> reason, boolean reasonRedacted, String httpMethod, String routeTemplate,
        AccessOutcome outcome, Instant occurredAt) {

    public AccessAuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(platformUserId, "platformUserId");
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(httpMethod, "httpMethod");
        Objects.requireNonNull(routeTemplate, "routeTemplate");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    /** A request let through by {@code session}. */
    public static AccessAuditEvent allowed(AccessSession session, String httpMethod, String routeTemplate,
            Instant now) {
        return new AccessAuditEvent(UUID.randomUUID(), Optional.of(session.id()), session.platformUserId(),
                session.tenant(), Optional.of(session.reason().value()), false, httpMethod, routeTemplate,
                AccessOutcome.ALLOWED, now);
    }

    /** A request of {@code platformUserId} on {@code tenant} refused for lack of a valid session or role. */
    public static AccessAuditEvent denied(UUID platformUserId, TenantId tenant, String httpMethod,
            String routeTemplate, Instant now) {
        return new AccessAuditEvent(UUID.randomUUID(), Optional.empty(), platformUserId, tenant, Optional.empty(),
                false, httpMethod, routeTemplate, AccessOutcome.DENIED, now);
    }
}
