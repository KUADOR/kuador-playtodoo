package com.kuador.platodo.api.audit.application;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog.AuditPage;
import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog.PageRequest;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerNotAuthenticatedException;
import com.kuador.platodo.api.identity.application.port.in.PlatformUserNames;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * {@code listComplexAuditEntries}: the SUPER_ADMIN reads the audited support requests to one complex, newest first,
 * purged complexes included. Entries older than the retention are never returned, even before a job deletes them. A
 * page costs the entries, their count and one query for the names of who made them.
 */
public final class ListAuditEntries {

    public static final int MAX_PAGE_SIZE = 100;

    private final CapabilityGuard guard;
    private final TenantDirectory tenants;
    private final AccessAuditLog auditLog;
    private final PlatformUserNames userNames;
    private final Clock clock;
    private final Duration retention;

    /** @param retention how long the audit is kept (ADR-016: one year by decision of the Kuador team) */
    public ListAuditEntries(CapabilityGuard guard, TenantDirectory tenants, AccessAuditLog auditLog,
            PlatformUserNames userNames, Clock clock, Duration retention) {
        this.guard = guard;
        this.tenants = tenants;
        this.auditLog = auditLog;
        this.userNames = userNames;
        this.clock = clock;
        this.retention = retention;
    }

    /**
     * @throws PlatformCallerNotAuthenticatedException if there is no authenticated caller
     * @throws PlatformCallerForbiddenException        if the caller is not the SUPER_ADMIN
     * @throws InvalidAuditPageException               if the page is out of the contract limits
     * @throws SupportComplexNotFoundException         if no complex has this id
     */
    public AuditEntries list(TenantId tenant, int page, int size) {
        guard.requireSuperAdmin();
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidAuditPageException();
        }
        if (tenants.find(tenant).isEmpty()) {
            throw new SupportComplexNotFoundException();
        }
        AuditPage found = auditLog.pageOf(tenant, clock.instant().minus(retention), new PageRequest(page, size));
        Map<UUID, String> names =
                userNames.displayNamesOf(found.items().stream().map(AccessAuditEvent::platformUserId).toList());
        return new AuditEntries(found.items().stream()
                .map(event -> new AuditEntry(event, nameOf(names, event)))
                .toList(), found.totalItems());
    }

    /** A platform user is never deleted, so a missing name is a broken reference of the global schema. */
    private static String nameOf(Map<UUID, String> names, AccessAuditEvent event) {
        String name = names.get(event.platformUserId());
        if (name == null) {
            throw new IllegalStateException("Unknown platform user " + event.platformUserId());
        }
        return name;
    }

    /** One audited request with the visible name of who made it. */
    public record AuditEntry(AccessAuditEvent event, String actorName) {

        public AuditEntry {
            Objects.requireNonNull(event, "event");
            Objects.requireNonNull(actorName, "actorName");
        }
    }

    public record AuditEntries(List<AuditEntry> items, long totalItems) {

        public AuditEntries {
            items = List.copyOf(items);
        }
    }
}
