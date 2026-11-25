package com.kuador.platodo.api.tenancy.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * A sports complex as Tenancy keeps it in {@code global.tenant}, with its lifecycle (ADR-013):
 * {@code PROVISIONING → ACTIVE ⇄ SUSPENDED}, {@code ACTIVE | SUSPENDED → ARCHIVED} and {@code ARCHIVED → ACTIVE}
 * before {@code purgeDueAt}. The purge states belong to the purge job. Every transition returns a new value; a
 * transition the current state does not allow throws {@link InvalidTransitionException}.
 *
 * <p>Entering {@code ACTIVE} clears the suspension and the archive, so the timestamps always describe the current
 * state: {@code suspendedAt} only while suspended (or archived after a suspension) and the archive fields only while
 * archived.
 *
 * @param retentionDays retention of the current archive; null unless {@code ARCHIVED}
 * @param purgeDueAt    instant from which the purge job may delete the schema; null unless {@code ARCHIVED}
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
    /** Shortest retention of an archive (ADR-013). */
    public static final int MIN_RETENTION_DAYS = 1;

    private static final Set<TenantStatus> ACTIVATABLE = Set.of(TenantStatus.PROVISIONING, TenantStatus.SUSPENDED);
    private static final Set<TenantStatus> ARCHIVABLE = Set.of(TenantStatus.ACTIVE, TenantStatus.SUSPENDED);

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

    /** {@code PROVISIONING | SUSPENDED → ACTIVE}. */
    public Tenant activate(Instant now) {
        requireStatusIn(ACTIVATABLE);
        return activeSince(now);
    }

    /** {@code ACTIVE → SUSPENDED}: its host answers "not available" to everyone. */
    public Tenant suspend(Instant now) {
        requireStatusIn(Set.of(TenantStatus.ACTIVE));
        return new Tenant(id, displayName, TenantStatus.SUSPENDED, createdAt, activatedAt, now, null, null, null,
                purgedAt);
    }

    /** {@code ACTIVE | SUSPENDED → ARCHIVED}, purgeable {@code retentionDays} after {@code now}. */
    public Tenant archive(Instant now, int retentionDays) {
        requireStatusIn(ARCHIVABLE);
        requireRetention(retentionDays);
        return archivedFor(now, retentionDays);
    }

    /**
     * {@code ARCHIVED → ACTIVE} while {@code now} is before {@code purgeDueAt}; the schema was kept, so the complex
     * recovers all its data.
     *
     * @throws RetentionExpiredException if the purge date has already been reached
     */
    public Tenant reactivate(Instant now) {
        requireStatusIn(Set.of(TenantStatus.ARCHIVED));
        if (!now.isBefore(purgeDueAt)) {
            throw new RetentionExpiredException();
        }
        return activeSince(now);
    }

    /**
     * Recomputes {@code purgeDueAt = archivedAt + days} of an archived complex.
     *
     * @throws RetentionInvalidException if the new purge date would not be after {@code now}
     */
    public Tenant changeRetention(int days, Instant now) {
        requireStatusIn(Set.of(TenantStatus.ARCHIVED));
        requireRetention(days);
        Tenant changed = archivedFor(archivedAt, days);
        if (!changed.purgeDueAt().isAfter(now)) {
            throw new RetentionInvalidException();
        }
        return changed;
    }

    public boolean is(TenantStatus expected) {
        return status == expected;
    }

    private Tenant activeSince(Instant now) {
        return new Tenant(id, displayName, TenantStatus.ACTIVE, createdAt, now, null, null, null, null, purgedAt);
    }

    private Tenant archivedFor(Instant archivedSince, int days) {
        return new Tenant(id, displayName, TenantStatus.ARCHIVED, createdAt, activatedAt, suspendedAt, archivedSince,
                days, archivedSince.plus(Duration.ofDays(days)), purgedAt);
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
