package com.kuador.platodo.api.audit.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.tenancy.application.port.out.PlatformAccessAuthorization;

/**
 * {@code RecordAccess} of the PLATODO-26 design (D6): checks the access session of a Kuador request on the host of a
 * complex and records the attempt, allowed or denied, before the request runs. The check and the record share one
 * transaction of their own, so the record is committed before the handler starts. If the record cannot be written
 * the request is denied: there is never an access without its audit.
 */
public final class AuthorizePlatformAccess implements PlatformAccessAuthorization {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizePlatformAccess.class);

    private final AccessSessions sessions;
    private final AccessAuditLog auditLog;
    private final TransactionOperations ownTransaction;
    private final Clock clock;

    /** {@code ownTransaction} must start a new transaction, independent of any the caller may have open. */
    public AuthorizePlatformAccess(AccessSessions sessions, AccessAuditLog auditLog,
            TransactionOperations ownTransaction, Clock clock) {
        this.sessions = sessions;
        this.auditLog = auditLog;
        this.ownTransaction = ownTransaction;
        this.clock = clock;
    }

    @Override
    public boolean authorize(PlatformAccessAttempt attempt) {
        try {
            return Boolean.TRUE.equals(ownTransaction.execute(status -> checkAndRecord(attempt)));
        } catch (RuntimeException auditFailure) {
            LOG.error("Kuador access to tenant {} denied: the access could not be audited",
                    attempt.tenant().value(), auditFailure);
            return false;
        }
    }

    private boolean checkAndRecord(PlatformAccessAttempt attempt) {
        Instant now = clock.instant();
        Optional<AccessSession> grantingSession = grantingSession(attempt, now);
        auditLog.append(grantingSession
                .map(session -> AccessAuditEvent.allowed(session, attempt.httpMethod(), attempt.routeTemplate(), now))
                .orElseGet(() -> AccessAuditEvent.denied(attempt.platformUserId(), attempt.tenant(),
                        attempt.httpMethod(), attempt.routeTemplate(), now)));
        return grantingSession.isPresent();
    }

    private Optional<AccessSession> grantingSession(PlatformAccessAttempt attempt, Instant now) {
        if (!attempt.platformOwner()) {
            return Optional.empty();
        }
        return attempt.sessionHeader()
                .flatMap(AuthorizePlatformAccess::sessionIdOf)
                .flatMap(sessions::find)
                .filter(session -> session.grants(attempt.platformUserId(), attempt.tenant(), now));
    }

    private static Optional<UUID> sessionIdOf(String header) {
        try {
            return Optional.of(UUID.fromString(header.strip()));
        } catch (IllegalArgumentException notASessionId) {
            return Optional.empty();
        }
    }
}
