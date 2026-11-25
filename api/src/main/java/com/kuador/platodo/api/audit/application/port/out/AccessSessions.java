package com.kuador.platodo.api.audit.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.audit.domain.AccessSession;

/** Access sessions of the Kuador owner, kept in the global schema. */
public interface AccessSessions {

    void add(AccessSession session);

    Optional<AccessSession> find(UUID sessionId);

    /** Marks the session as closed at {@code closedAt}. */
    void close(UUID sessionId, Instant closedAt);
}
