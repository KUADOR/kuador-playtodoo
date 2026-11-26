package com.kuador.platodo.api.identity.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Which delegable capabilities operators have, and the version of the whole set for optimistic concurrency. A
 * capability without a stored setting is disabled.
 *
 * @param version version the caller must send back to change the settings
 * @param enabled whether operators have each delegable capability
 */
public record OperatorCapabilitySettings(long version, Map<Capability, Boolean> enabled) {

    public OperatorCapabilitySettings {
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        enabled = Collections.unmodifiableMap(new EnumMap<>(Objects.requireNonNull(enabled, "enabled")));
    }

    public boolean isEnabled(Capability capability) {
        return enabled.getOrDefault(capability, false);
    }

    /** The capabilities operators have. */
    public Set<Capability> enabledCapabilities() {
        Set<Capability> result = EnumSet.noneOf(Capability.class);
        enabled.forEach((capability, on) -> {
            if (on) {
                result.add(capability);
            }
        });
        return result;
    }

    /** The requested settings that differ from the current ones: only those are applied and recorded. */
    public List<CapabilityToggle> effectiveChanges(List<CapabilityToggle> requested) {
        return requested.stream()
                .filter(toggle -> isEnabled(toggle.capability()) != toggle.enabled())
                .toList();
    }
}
