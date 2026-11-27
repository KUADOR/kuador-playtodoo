package com.kuador.platodo.api.tenancy.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * A sports complex as Tenancy keeps it in {@code global.tenant}, with its lifecycle (ADR-013 as adjusted by ADR-014):
 * {@code PROVISIONING ⇄ PROVISIONING_FAILED}, {@code PROVISIONING → ACTIVE ⇄ SUSPENDED} (reactivation only before
 * {@code purgeDueAt}), {@code ACTIVE | SUSPENDED → ARCHIVED} (terminal: a cancelled subscription is never reactivated)
 * and, for the purge job, {@code ARCHIVED → PURGING → PURGED} from {@code purgeDueAt} on. Every transition returns a
 * new value; a transition the current state does not allow throws {@link InvalidTransitionException}.
 *
 * <p>Suspending and archiving an active complex start its retention; archiving a suspended one keeps the retention
 * that was running. Entering {@code ACTIVE} clears the suspension, so the timestamps always describe the current
 * state: {@code suspendedAt} only while suspended (or archived after a suspension) and {@code archivedAt} only while
 * archived.
 *
 * @param retentionDays retention of the current suspension or archive; null unless {@code SUSPENDED} or later
 * @param purgeDueAt    instant from which the purge job may delete the schema; null unless {@code SUSPENDED} or later
 * @param purgedAt      instant the schema was deleted; null unless {@code PURGED}
 */
public record Tenant(
        TenantId id,
        String displayName,
        TenantStatus status,
        Instant createdAt,
        Instant activatedAt,
        Instant suspendedAt,
        Instant archivedAt,
        Integer retentionDays,
        Instant purgeDueAt,
        Instant purgedAt) {

    /** Longest commercial name, as {@code global.tenant.display_name} stores it. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 120;
    /** Shortest retention of a suspension or an archive (ADR-013). */
    public static final int MIN_RETENTION_DAYS = 1;

    private static final Set<TenantStatus> ARCHIVABLE = Set.of(TenantStatus.ACTIVE, TenantStatus.SUSPENDED);
    private static final Set<TenantStatus> IN_RETENTION = Set.of(TenantStatus.SUSPENDED, TenantStatus.ARCHIVED);

    public Tenant {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        requireDisplayName(displayName);
    }

    /** A complex just registered: its schema and owner are still to be provisioned. */
    public static Tenant provisioning(TenantId id, String displayName, Instant now) {
        return new Tenant(id, displayName, TenantStatus.PROVISIONING, now, null, null, null, null, null, null);
    }

    /** {@code PROVISIONING → PROVISIONING_FAILED}: its schema or owner could not be created; it waits for a retry. */
    public Tenant failProvisioning() {
        requireStatusIn(Set.of(TenantStatus.PROVISIONING));
        return withStatus(TenantStatus.PROVISIONING_FAILED, purgedAt);
    }

    /** {@code PROVISIONING_FAILED → PROVISIONING}: a retry of the provisioning starts. */
    public Tenant retryProvisioning() {
        requireStatusIn(Set.of(TenantStatus.PROVISIONING_FAILED));
        return withStatus(TenantStatus.PROVISIONING, purgedAt);
    }

    /** {@code PROVISIONING → ACTIVE}: its schema and owner exist. */
    public Tenant activate(Instant now) {
        requireStatusIn(Set.of(TenantStatus.PROVISIONING));
        return activeSince(now);
    }

    /**
     * {@code ACTIVE → SUSPENDED}: its host answers "not available" to everyone and it is purgeable
     * {@code retentionDays} after {@code now}.
     */
    public Tenant suspend(Instant now, int retentionDays) {
        requireStatusIn(Set.of(TenantStatus.ACTIVE));
        requireRetention(retentionDays);
        return new Tenant(id, displayName, TenantStatus.SUSPENDED, createdAt, activatedAt, now, null, retentionDays,
                now.plus(Duration.ofDays(retentionDays)), purgedAt);
    }

    /**
     * {@code ACTIVE | SUSPENDED → ARCHIVED}. An active complex is purgeable {@code retentionDays} after {@code now};
     * a suspended one keeps the retention and purge date of its suspension.
     */
    public Tenant archive(Instant now, int retentionDays) {
        requireStatusIn(ARCHIVABLE);
        requireRetention(retentionDays);
        if (is(TenantStatus.SUSPENDED)) {
            return archivedAt(now, this.retentionDays, purgeDueAt);
        }
        return archivedAt(now, retentionDays, now.plus(Duration.ofDays(retentionDays)));
    }

    /**
     * {@code SUSPENDED → ACTIVE} while {@code now} is before {@code purgeDueAt}; the schema was kept, so the complex
     * recovers all its data.
     *
     * @throws RetentionExpiredException if the purge date has already been reached
     */
    public Tenant reactivate(Instant now) {
        requireStatusIn(Set.of(TenantStatus.SUSPENDED));
        if (!now.isBefore(purgeDueAt)) {
            throw new RetentionExpiredException();
        }
        return activeSince(now);
    }

    /**
     * Recomputes {@code purgeDueAt = retention start + days} of a suspended or archived complex. The retention starts
     * with the suspension, or with the archive when the complex was cancelled while active.
     *
     * @throws NotInRetentionException  if the complex is neither suspended nor archived
     * @throws RetentionInvalidException if the new purge date would not be after {@code now}
     */
    public Tenant changeRetention(int days, Instant now) {
        if (!IN_RETENTION.contains(status)) {
            throw new NotInRetentionException();
        }
        requireRetention(days);
        Instant newPurgeDueAt = retentionStart().plus(Duration.ofDays(days));
        if (!newPurgeDueAt.isAfter(now)) {
            throw new RetentionInvalidException();
        }
        return new Tenant(id, displayName, status, createdAt, activatedAt, suspendedAt, archivedAt, days,
                newPurgeDueAt, purgedAt);
    }

    /**
     * {@code ARCHIVED → PURGING} once {@code purgeDueAt} is reached: the complex can no longer be reactivated and its
     * host answers "not found" while the purge job deletes its schema. The archive fields are kept.
     *
     * @throws InvalidTransitionException if the complex is not archived or its purge date is still ahead
     */
    public Tenant startPurge(Instant now) {
        requireStatusIn(Set.of(TenantStatus.ARCHIVED));
        if (now.isBefore(purgeDueAt)) {
            throw new InvalidTransitionException();
        }
        return withStatus(TenantStatus.PURGING, purgedAt);
    }

    /**
     * {@code PURGING → PURGED}: the schema was deleted. What is left is the tombstone of the complex (ADR-013): its
     * id, commercial name, state and dates.
     */
    public Tenant markPurged(Instant now) {
        requireStatusIn(Set.of(TenantStatus.PURGING));
        return withStatus(TenantStatus.PURGED, now);
    }

    public boolean is(TenantStatus expected) {
        return status == expected;
    }

    private Tenant activeSince(Instant now) {
        return new Tenant(id, displayName, TenantStatus.ACTIVE, createdAt, now, null, null, null, null, purgedAt);
    }

    private Tenant withStatus(TenantStatus next, Instant purged) {
        return new Tenant(id, displayName, next, createdAt, activatedAt, suspendedAt, archivedAt, retentionDays,
                purgeDueAt, purged);
    }

    private Tenant archivedAt(Instant archivedSince, Integer days, Instant purgeDue) {
        return new Tenant(id, displayName, TenantStatus.ARCHIVED, createdAt, activatedAt, suspendedAt, archivedSince,
                days, purgeDue, purgedAt);
    }

    private Instant retentionStart() {
        return suspendedAt != null ? suspendedAt : archivedAt;
    }

    private void requireStatusIn(Set<TenantStatus> allowed) {
        if (!allowed.contains(status)) {
            throw new InvalidTransitionException();
        }
    }

    private static void requireRetention(int days) {
        if (days < MIN_RETENTION_DAYS) {
            throw new IllegalArgumentException("The retention must be at least " + MIN_RETENTION_DAYS + " day");
        }
    }

    private static void requireDisplayName(String displayName) {
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank() || displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "The display name must have 1 to " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
    }
}
