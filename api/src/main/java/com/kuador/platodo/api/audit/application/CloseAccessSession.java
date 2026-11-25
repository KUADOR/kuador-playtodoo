package com.kuador.platodo.api.audit.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.application.port.out.CurrentPlatformOwner;
import com.kuador.platodo.api.audit.domain.AccessSession;

/** The Kuador owner closes one of their access sessions before it expires; closing it again changes nothing. */
public final class CloseAccessSession {

    private final CurrentPlatformOwner owner;
    private final AccessSessions sessions;
    private final TransactionOperations transactions;
    private final Clock clock;

    public CloseAccessSession(CurrentPlatformOwner owner, AccessSessions sessions, TransactionOperations transactions,
            Clock clock) {
        this.owner = owner;
        this.sessions = sessions;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * @throws PlatformUserNotAuthenticatedException if there is no authenticated caller
     * @throws NotPlatformOwnerException             if the caller is not the Kuador owner
     * @throws AccessSessionNotFoundException        if the caller has no session with this id
     */
    public void close(UUID sessionId) {
        UUID platformUserId = owner.require();
        transactions.executeWithoutResult(status -> {
            AccessSession session = sessions.find(sessionId)
                    .filter(found -> found.isOwnedBy(platformUserId))
                    .orElseThrow(AccessSessionNotFoundException::new);
            Instant now = clock.instant();
            if (session.isOpenAt(now)) {
                sessions.close(sessionId, now);
            }
        });
    }
}
