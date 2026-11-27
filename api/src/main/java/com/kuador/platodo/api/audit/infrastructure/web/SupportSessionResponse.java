package com.kuador.platodo.api.audit.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.OpenSupportSession.OpenedSession;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** {@code SupportSession} of the contract. */
record SupportSessionResponse(UUID sessionId, Instant expiresAt, String tenantHost, TenantStatus tenantStatus) {

    static SupportSessionResponse of(OpenedSession session) {
        return new SupportSessionResponse(session.sessionId(), session.expiresAt(), session.tenantHost(),
                session.tenantStatus());
    }
}
