package com.kuador.platodo.api.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * One audited support request of a Kuador platform user on a complex (ADR-011 D12, ADR-020): the opening of a session
 * or a request on the host of the complex. It keeps the route template and operationId, never the concrete path nor
 * the query string. A denied request carries no session nor reason: the session, if any, was not valid for this
 * complex and its reason may belong to another one.
 *
 * @param reasonRedacted whether the reason was anonymized by the purge of the complex
 * @param denialCode     problem code answered to a denied request; empty when it was allowed
 */
public record AccessAuditEvent(UUID id, Optional<UUID> sessionId, UUID platformUserId, TenantId tenant,
        Optional<String> reason, boolean reasonRedacted, AuditedRequest request, AccessOutcome outcome,
        Optional<DenialCode> denialCode, Instant occurredAt) {

    public AccessAuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(platformUserId, "platformUserId");
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(denialCode, "denialCode");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if ((outcome == AccessOutcome.DENIED) != denialCode.isPresent()) {
            throw new IllegalArgumentException("A denied request, and only a denied one, has a denial code");
        }
    }

    /** A request let through by {@code session}, or the opening of {@code session} itself. */
    public static AccessAuditEvent allowed(AccessSession session, AuditedRequest request, Instant now) {
        return new AccessAuditEvent(UUID.randomUUID(), Optional.of(session.id()), session.platformUserId(),
                session.tenant(), Optional.of(session.reason().value()), false, request, AccessOutcome.ALLOWED,
                Optional.empty(), now);
    }

    /** A request of {@code platformUserId} on {@code tenant} refused with {@code code}. */
    public static AccessAuditEvent denied(UUID platformUserId, TenantId tenant, AuditedRequest request,
            DenialCode code, Instant now) {
        return new AccessAuditEvent(UUID.randomUUID(), Optional.empty(), platformUserId, tenant, Optional.empty(),
                false, request, AccessOutcome.DENIED, Optional.of(code), now);
    }
}
