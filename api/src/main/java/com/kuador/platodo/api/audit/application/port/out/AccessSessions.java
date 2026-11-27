package com.kuador.platodo.api.audit.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Support sessions of the Kuador team (ADR-020), kept in the global schema. Writes need a global transaction. */
public interface AccessSessions {

    void add(AccessSession session);

    Optional<AccessSession> find(UUID sessionId);

    /** Marks the session as closed at {@code closedAt}. */
    void close(UUID sessionId, Instant closedAt);

    /**
     * Closes at {@code closedAt} every session of {@code tenant} still open then; sessions of other complexes are
     * untouched.
     *
     * @return how many sessions were closed
     */
    int closeOpenOf(TenantId tenant, Instant closedAt);
}
