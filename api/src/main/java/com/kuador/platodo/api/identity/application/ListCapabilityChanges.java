package com.kuador.platodo.api.identity.application;

import java.time.Clock;
import java.time.Duration;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.ChangePage;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.PageRequest;

/** {@code GET /operator-capabilities/changes}: SUPER_ADMIN only; newest first, within the retention period. */
public final class ListCapabilityChanges {

    /** Capability changes are kept for one year (ADR-017; decision of the Kuador team). */
    private static final Duration RETENTION = Duration.ofDays(365);

    private final CapabilityGuard guard;
    private final CapabilityChangeLog changeLog;
    private final Clock clock;

    public ListCapabilityChanges(CapabilityGuard guard, CapabilityChangeLog changeLog, Clock clock) {
        this.guard = guard;
        this.changeLog = changeLog;
        this.clock = clock;
    }

    public ChangePage list(PageRequest page) {
        guard.requireSuperAdmin();
        return changeLog.pageSince(clock.instant().minus(RETENTION), page);
    }
}
