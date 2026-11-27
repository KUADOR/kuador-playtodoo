package com.kuador.platodo.api.audit.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.audit.domain.AuditedRequest;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerNotAuthenticatedException;

/**
 * {@code closeSupportSession}: the Kuador team member who opened a support session closes it before it expires, and
 * the closing is audited with the session. Closing it again, or once expired, changes nothing and records nothing.
 */
public final class CloseSupportSession {

    private final CapabilityGuard guard;
    private final AccessSessions sessions;
    private final AccessAuditLog auditLog;
    private final TransactionOperations transactions;
    private final Clock clock;

    /** The ports the closing needs. */
    public record Ports(CapabilityGuard guard, AccessSessions sessions, AccessAuditLog auditLog,
            TransactionOperations transactions) {
    }

    public CloseSupportSession(Ports ports, Clock clock) {
        this.guard = ports.guard();
        this.sessions = ports.sessions();
        this.auditLog = ports.auditLog();
        this.transactions = ports.transactions();
        this.clock = clock;
    }

    /**
     * @param closing what the audit keeps of the closing request
     * @throws PlatformCallerNotAuthenticatedException if there is no authenticated caller
     * @throws PlatformCallerForbiddenException        if the caller is not a Kuador team member
     * @throws SupportSessionNotFoundException         if the caller has no session with this id
     */
    public void close(UUID sessionId, AuditedRequest closing) {
        UUID platformUserId = guard.requirePlatformUser().id();
        transactions.executeWithoutResult(status -> {
            AccessSession session = sessions.find(sessionId)
                    .filter(found -> found.isOwnedBy(platformUserId))
                    .orElseThrow(SupportSessionNotFoundException::new);
            Instant now = clock.instant();
            if (session.isOpenAt(now)) {
                sessions.close(sessionId, now);
                auditLog.append(AccessAuditEvent.allowed(session, closing, now));
            }
        });
    }
}
