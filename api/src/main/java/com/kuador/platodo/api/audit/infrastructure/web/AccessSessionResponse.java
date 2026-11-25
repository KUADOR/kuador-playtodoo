package com.kuador.platodo.api.audit.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.OpenAccessSession.OpenedSession;

/** {@code AccessSession} of the contract. */
record AccessSessionResponse(UUID sessionId, Instant expiresAt, String tenantHost) {

    static AccessSessionResponse of(OpenedSession session) {
        return new AccessSessionResponse(session.sessionId(), session.expiresAt(), session.tenantHost());
    }
}
