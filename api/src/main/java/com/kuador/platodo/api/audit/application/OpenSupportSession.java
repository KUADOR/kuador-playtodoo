package com.kuador.platodo.api.audit.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.audit.domain.AuditedRequest;
import com.kuador.platodo.api.audit.domain.DenialCode;
import com.kuador.platodo.api.audit.domain.SupportCaller;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerNotAuthenticatedException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantEntry;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * {@code openSupportSession} (ADR-003, ADR-020): a Kuador team member opens a support session on one complex with a
 * mandatory reason. Whether the role allows the status of the complex is checked here and again on every request
 * made with the session. The opening is audited, allowed or refused, before answering.
 */
public final class OpenSupportSession {

    private final CurrentSupportCaller callers;
    private final TenantDirectory tenants;
    private final AccessSessions sessions;
    private final AccessAuditLog auditLog;
    private final TransactionOperations transactions;
    private final Duration timeToLive;
    private final Clock clock;

    /** The ports the opening needs. */
    public record Ports(CurrentSupportCaller callers, TenantDirectory tenants, AccessSessions sessions,
            AccessAuditLog auditLog, TransactionOperations transactions) {
    }

    public OpenSupportSession(Ports ports, Duration timeToLive, Clock clock) {
        if (timeToLive.isNegative() || timeToLive.isZero()) {
            throw new IllegalArgumentException("The lifetime of a support session must be positive");
        }
        this.callers = ports.callers();
        this.tenants = ports.tenants();
        this.sessions = ports.sessions();
        this.auditLog = ports.auditLog();
        this.transactions = ports.transactions();
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    /**
     * @param opening what the audit keeps of the opening request
     * @throws PlatformCallerNotAuthenticatedException if there is no authenticated caller
     * @throws PlatformCallerForbiddenException        if the caller is not a Kuador team member
     * @throws SupportComplexNotFoundException         if no complex has this id
     * @throws SupportSessionRefusedException          if the capability, the status or the reason does not allow it
     */
    public OpenedSession open(TenantId tenantId, String reason, AuditedRequest opening) {
        SupportCaller caller = callers.require();
        TenantEntry tenant = tenants.find(tenantId).orElseThrow(SupportComplexNotFoundException::new);
        Instant now = clock.instant();
        Optional<AccessReason> accessReason = AccessReason.parse(reason);
        Optional<DenialCode> refusal = caller.refusalOn(tenant.status())
                .or(() -> accessReason.isEmpty() ? Optional.of(DenialCode.REASON_REQUIRED) : Optional.empty());
        if (refusal.isPresent()) {
            AccessAuditEvent denial = AccessAuditEvent.denied(caller.id(), tenantId, opening, refusal.get(), now);
            transactions.executeWithoutResult(status -> auditLog.append(denial));
            throw new SupportSessionRefusedException(refusal.get());
        }
        String tenantHost = tenant.host().orElseThrow(SupportComplexNotFoundException::new);
        AccessSession session = AccessSession.open(caller.id(), tenantId, accessReason.get(), now, timeToLive);
        transactions.executeWithoutResult(status -> {
            sessions.add(session);
            auditLog.append(AccessAuditEvent.allowed(session, opening, now));
        });
        return new OpenedSession(session.id(), session.expiresAt(), tenantHost, tenant.status());
    }

    /** What the backoffice needs to call the host of the complex. */
    public record OpenedSession(UUID sessionId, Instant expiresAt, String tenantHost, TenantStatus tenantStatus) {
    }
}
