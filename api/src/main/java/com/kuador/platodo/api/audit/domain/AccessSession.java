package com.kuador.platodo.api.audit.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Time-boxed permission of one Kuador platform user on the data of one complex (ADR-011). It grants nothing once it
 * expires or is closed, nor to another user or on another complex.
 */
public record AccessSession(UUID id, UUID platformUserId, TenantId tenant, AccessReason reason, Instant startedAt,
        Instant expiresAt, Optional<Instant> closedAt) {

    public AccessSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(platformUserId, "platformUserId");
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(closedAt, "closedAt");
        if (!expiresAt.isAfter(startedAt)) {
            throw new IllegalArgumentException("An access session must expire after it starts");
        }
    }

    /** A new session of {@code platformUserId} on {@code tenant}, open from {@code now} for {@code timeToLive}. */
    public static AccessSession open(UUID platformUserId, TenantId tenant, AccessReason reason, Instant now,
            Duration timeToLive) {
        return new AccessSession(UUID.randomUUID(), platformUserId, tenant, reason, now, now.plus(timeToLive),
                Optional.empty());
    }

    /** Neither closed nor expired at {@code now}. */
    public boolean isOpenAt(Instant now) {
        return closedAt.isEmpty() && now.isBefore(expiresAt);
    }

    /** Whether it lets {@code platformUserId} reach the data of {@code requestedTenant} at {@code now}. */
    public boolean grants(UUID platformUserId, TenantId requestedTenant, Instant now) {
        return isOpenAt(now) && this.platformUserId.equals(platformUserId) && tenant.equals(requestedTenant);
    }

    public boolean isOwnedBy(UUID platformUserId) {
        return this.platformUserId.equals(platformUserId);
    }
}
