package com.kuador.platodo.api.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog;
import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilityEvents;
import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilityUpdates;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.CapabilityToggle;
import com.kuador.platodo.api.identity.domain.OperatorCapabilitySettings;
import com.kuador.platodo.api.identity.domain.event.OperatorCapabilitiesChanged;

/**
 * {@code PUT /operator-capabilities}: SUPER_ADMIN only. Applies a batch with optimistic concurrency: the settings are
 * locked, the version is compared and only the effective changes are stored, recorded and published, all in one
 * transaction. A batch that changes nothing keeps the version and records nothing.
 */
public final class ChangeOperatorCapabilities {

    private final CapabilityGuard guard;
    private final OperatorCapabilityUpdates updates;
    private final CapabilityChangeLog changeLog;
    private final OperatorCapabilityEvents events;
    private final TransactionOperations globalTransaction;
    private final Clock clock;

    public ChangeOperatorCapabilities(CapabilityGuard guard, OperatorCapabilityUpdates updates,
            CapabilityChangeLog changeLog, OperatorCapabilityEvents events, TransactionOperations globalTransaction,
            Clock clock) {
        this.guard = guard;
        this.updates = updates;
        this.changeLog = changeLog;
        this.events = events;
        this.globalTransaction = globalTransaction;
        this.clock = clock;
    }

    /**
     * @throws InvalidCapabilityChangeException   if {@code requested} is empty or repeats a capability
     * @throws StaleOperatorCapabilitiesException if {@code expectedVersion} is not the current version
     */
    public OperatorCapabilitySettings change(long expectedVersion, List<CapabilityToggle> requested) {
        PlatformCaller caller = guard.requireSuperAdmin();
        requireOneChangePerCapability(requested);
        return globalTransaction.execute(status -> apply(caller, expectedVersion, requested));
    }

    private OperatorCapabilitySettings apply(PlatformCaller caller, long expectedVersion,
            List<CapabilityToggle> requested) {
        OperatorCapabilitySettings current = updates.lockCurrent();
        if (current.version() != expectedVersion) {
            throw new StaleOperatorCapabilitiesException();
        }
        List<CapabilityToggle> effective = current.effectiveChanges(requested);
        if (effective.isEmpty()) {
            return current;
        }
        long newVersion = current.version() + 1;
        Instant now = clock.instant();
        updates.apply(effective, newVersion);
        changeLog.append(effective, caller.id(), now);
        events.publish(new OperatorCapabilitiesChanged(effective, caller.id(), now));
        return new OperatorCapabilitySettings(newVersion, withChanges(current, effective));
    }

    private static Map<Capability, Boolean> withChanges(OperatorCapabilitySettings current,
            List<CapabilityToggle> changes) {
        Map<Capability, Boolean> enabled = new EnumMap<>(Capability.class);
        enabled.putAll(current.enabled());
        changes.forEach(change -> enabled.put(change.capability(), change.enabled()));
        return enabled;
    }

    private static void requireOneChangePerCapability(List<CapabilityToggle> requested) {
        if (requested.isEmpty()) {
            throw new InvalidCapabilityChangeException("At least one change is required");
        }
        long distinct = requested.stream().map(CapabilityToggle::capability).distinct().count();
        if (distinct != requested.size()) {
            throw new InvalidCapabilityChangeException("Each capability may appear only once");
        }
    }
}
