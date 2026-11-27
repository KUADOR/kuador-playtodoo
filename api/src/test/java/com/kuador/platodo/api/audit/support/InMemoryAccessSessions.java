package com.kuador.platodo.api.audit.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Access sessions kept in memory for application tests. */
public final class InMemoryAccessSessions implements AccessSessions {

    private final Map<UUID, AccessSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void add(AccessSession session) {
        sessions.put(session.id(), session);
    }

    @Override
    public Optional<AccessSession> find(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void close(UUID sessionId, Instant closedAt) {
        sessions.computeIfPresent(sessionId, (id, session) -> new AccessSession(id, session.platformUserId(),
                session.tenant(), session.reason(), session.startedAt(), session.expiresAt(), Optional.of(closedAt)));
    }

    @Override
    public int closeOpenOf(TenantId tenant, Instant closedAt) {
        List<UUID> open = sessions.values().stream()
                .filter(session -> session.tenant().equals(tenant) && session.isOpenAt(closedAt))
                .map(AccessSession::id)
                .toList();
        open.forEach(id -> close(id, closedAt));
        return open.size();
    }
}
