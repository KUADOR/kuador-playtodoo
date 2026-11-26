package com.kuador.platodo.api.identity.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog;
import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilities;
import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilityUpdates;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.CapabilityToggle;
import com.kuador.platodo.api.identity.domain.OperatorCapabilitySettings;

/** Operator capabilities and their change log in memory; every capability starts disabled, at version 0. */
public final class InMemoryOperatorCapabilities
        implements OperatorCapabilities, OperatorCapabilityUpdates, CapabilityChangeLog {

    private static final String CHANGED_BY_NAME = "In-memory user";

    private final Map<Capability, Boolean> enabled = new EnumMap<>(Capability.class);
    private final List<RecordedChange> changes = new ArrayList<>();
    private long version;

    public InMemoryOperatorCapabilities() {
        for (Capability capability : Capability.values()) {
            enabled.put(capability, false);
        }
    }

    /** Sets a capability directly, as another request of the SUPER_ADMIN would. */
    public void set(Capability capability, boolean on) {
        enabled.put(capability, on);
        version++;
    }

    public List<RecordedChange> recorded() {
        return List.copyOf(changes);
    }

    @Override
    public synchronized OperatorCapabilitySettings current() {
        return new OperatorCapabilitySettings(version, enabled);
    }

    @Override
    public OperatorCapabilitySettings lockCurrent() {
        return current();
    }

    @Override
    public synchronized void apply(List<CapabilityToggle> toggles, long newVersion) {
        toggles.forEach(toggle -> enabled.put(toggle.capability(), toggle.enabled()));
        version = newVersion;
    }

    @Override
    public synchronized void append(List<CapabilityToggle> toggles, UUID changedBy, Instant changedAt) {
        toggles.forEach(toggle -> changes.add(new RecordedChange(
                UUID.randomUUID(), toggle.capability(), toggle.enabled(), changedBy, CHANGED_BY_NAME, changedAt)));
    }

    @Override
    public ChangePage pageSince(Instant since, PageRequest page) {
        List<RecordedChange> matching = changes.reversed().stream()
                .filter(change -> !change.changedAt().isBefore(since))
                .toList();
        List<RecordedChange> items = matching.stream().skip(page.offset()).limit(page.size()).toList();
        return new ChangePage(items, matching.size());
    }
}
