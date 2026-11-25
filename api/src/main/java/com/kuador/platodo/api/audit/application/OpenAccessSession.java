package com.kuador.platodo.api.audit.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.application.port.out.AccessSessions;
import com.kuador.platodo.api.audit.application.port.out.CurrentPlatformOwner;
import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.audit.domain.ReasonRequiredException;
import com.kuador.platodo.api.audit.domain.ReasonTooLongException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * The Kuador owner opens an access session to one complex, with a mandatory reason (ADR-003, ADR-011). The session is
 * valid only on the host of that complex and for the configured lifetime.
 */
public final class OpenAccessSession {

    private final CurrentPlatformOwner owner;
    private final TenantDirectory tenants;
    private final AccessSessions sessions;
    private final TransactionOperations transactions;
    private final Duration timeToLive;
    private final Clock clock;

    public OpenAccessSession(CurrentPlatformOwner owner, TenantDirectory tenants, AccessSessions sessions,
            TransactionOperations transactions, Duration timeToLive, Clock clock) {
        if (timeToLive.isNegative() || timeToLive.isZero()) {
            throw new IllegalArgumentException("The lifetime of an access session must be positive");
        }
        this.owner = owner;
        this.tenants = tenants;
        this.sessions = sessions;
        this.transactions = transactions;
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    /**
     * @throws PlatformUserNotAuthenticatedException if there is no authenticated caller
     * @throws NotPlatformOwnerException             if the caller is not the Kuador owner
     * @throws ReasonRequiredException               if the reason is missing or blank
     * @throws ReasonTooLongException                if the reason is too long
     * @throws AccessTenantNotFoundException         if no reachable complex has this id
     */
    public OpenedSession open(TenantId tenant, String reason) {
        UUID platformUserId = owner.require();
        AccessReason accessReason = new AccessReason(reason);
        String tenantHost = tenants.find(tenant)
                .flatMap(TenantDirectory.TenantEntry::host)
                .orElseThrow(AccessTenantNotFoundException::new);
        Instant now = clock.instant();
        AccessSession session = AccessSession.open(platformUserId, tenant, accessReason, now, timeToLive);
        transactions.executeWithoutResult(status -> sessions.add(session));
        return new OpenedSession(session.id(), session.expiresAt(), tenantHost);
    }

    /** What the backoffice needs to call the host of the complex. */
    public record OpenedSession(UUID sessionId, Instant expiresAt, String tenantHost) {
    }
}
