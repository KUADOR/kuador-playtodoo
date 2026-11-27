package com.kuador.platodo.api.identity.application;

import java.time.Clock;
import java.time.Duration;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.ChangePage;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.PageRequest;

/**
 * {@code GET /operator-capabilities/changes}: SUPER_ADMIN only; newest first, within the retention period, so older
 * changes are never returned even before the scheduled job deletes them.
 */
public final class ListCapabilityChanges {

    private final CapabilityGuard guard;
    private final CapabilityChangeLog changeLog;
    private final Clock clock;
    private final Duration retention;

    /** @param retention how long capability changes are kept (ADR-017: one year by decision of the Kuador team) */
    public ListCapabilityChanges(CapabilityGuard guard, CapabilityChangeLog changeLog, Clock clock,
            Duration retention) {
        this.guard = guard;
        this.changeLog = changeLog;
        this.clock = clock;
        this.retention = retention;
    }

    public ChangePage list(PageRequest page) {
        guard.requireSuperAdmin();
        return changeLog.pageSince(clock.instant().minus(retention), page);
    }
}
