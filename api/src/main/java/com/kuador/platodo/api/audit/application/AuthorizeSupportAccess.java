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
import com.kuador.platodo.api.audit.domain.AuditedRequest;
import com.kuador.platodo.api.audit.domain.DenialCode;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;
import com.kuador.platodo.api.tenancy.application.port.in.TenantProfileLock;
import com.kuador.platodo.api.tenancy.application.port.out.SupportAccessAuthorization;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * {@code RecordAccess} of the PLATODO-26 design (D6) as adjusted by ADR-020: checks the support session of a Kuador
 * request on the host of a complex, and that the role of the caller still allows the status of the complex, and
 * records the attempt, allowed or denied, before the request runs. The check and the record share one transaction of
 * their own, so the record is committed before the handler starts. If the record cannot be written the request is
 * denied: there is never an access without its audit.
 *
 * <p>The status is read holding the registry row of the complex, so a purge or lifecycle change in progress is waited
 * for and its outcome decides: a complex purged meanwhile is denied instead of reached. A purge only starts once the
 * complex is {@code PURGING}, whose host no request reaches.
 */
public final class AuthorizeSupportAccess implements SupportAccessAuthorization {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizeSupportAccess.class);

    private final CurrentSupportCaller callers;
    private final TenantProfileLock tenants;
    private final AccessSessions sessions;
    private final AccessAuditLog auditLog;
    private final TransactionOperations ownTransaction;
    private final Clock clock;

    /** The ports the check needs; {@code ownTransaction} must start a transaction independent of any open one. */
    public record Ports(CurrentSupportCaller callers, TenantProfileLock tenants, AccessSessions sessions,
            AccessAuditLog auditLog, TransactionOperations ownTransaction) {
    }

    public AuthorizeSupportAccess(Ports ports, Clock clock) {
        this.callers = ports.callers();
        this.tenants = ports.tenants();
        this.sessions = ports.sessions();
        this.auditLog = ports.auditLog();
        this.ownTransaction = ports.ownTransaction();
        this.clock = clock;
    }

    @Override
    public boolean authorize(SupportAccessAttempt attempt) {
        try {
            return Boolean.TRUE.equals(ownTransaction.execute(status -> checkAndRecord(attempt)));
        } catch (RuntimeException auditFailure) {
            LOG.error("Kuador access to tenant {} denied: the access could not be audited",
                    attempt.tenant().value(), auditFailure);
            return false;
        }
    }

    private boolean checkAndRecord(SupportAccessAttempt attempt) {
        Instant now = clock.instant();
        AuditedRequest request =
                new AuditedRequest(attempt.httpMethod(), attempt.routeTemplate(), attempt.operationId());
        Optional<AccessSession> grantingSession = grantingSession(attempt, now);
        auditLog.append(grantingSession
                .map(session -> AccessAuditEvent.allowed(session, request, now))
                .orElseGet(() -> AccessAuditEvent.denied(attempt.platformUserId(), attempt.tenant(), request,
                        DenialCode.SUPPORT_SESSION_INVALID, now)));
        return grantingSession.isPresent();
    }

    private Optional<AccessSession> grantingSession(SupportAccessAttempt attempt, Instant now) {
        Optional<TenantStatus> status = tenants.lock(attempt.tenant()).map(TenantProfile::status);
        if (status.isEmpty() || !callerMayWorkOn(status.get())) {
            return Optional.empty();
        }
        return attempt.sessionHeader()
                .flatMap(AuthorizeSupportAccess::sessionIdOf)
                .flatMap(sessions::find)
                .filter(session -> session.grants(attempt.platformUserId(), attempt.tenant(), now));
    }

    /** A platform role this code does not know may not work on any complex. */
    private boolean callerMayWorkOn(TenantStatus status) {
        try {
            return callers.require().refusalOn(status).isEmpty();
        } catch (PlatformCallerForbiddenException unknownRole) {
            return false;
        }
    }

    private static Optional<UUID> sessionIdOf(String header) {
        try {
            return Optional.of(UUID.fromString(header.strip()));
        } catch (IllegalArgumentException notASessionId) {
            return Optional.empty();
        }
    }
}
