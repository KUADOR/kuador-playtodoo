package com.kuador.platodo.api.identity.infrastructure.web;

import java.util.List;

import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.CapabilityToggle;

/**
 * Schema {@code ChangeOperatorCapabilitiesRequest}. Missing or out-of-range values fail with
 * {@link InvalidPlatformRequestException}; an unknown capability key fails while reading the body.
 */
record ChangeOperatorCapabilitiesRequest(Long version, List<Change> changes) {

    record Change(Capability key, Boolean enabled) {
    }

    long requiredVersion() {
        if (version == null || version < 0) {
            throw new InvalidPlatformRequestException("`version` is required and must not be negative.");
        }
        return version;
    }

    List<CapabilityToggle> toggles() {
        if (changes == null || changes.stream().anyMatch(ChangeOperatorCapabilitiesRequest::incomplete)) {
            throw new InvalidPlatformRequestException(
                    "`changes` is required and every change needs `key` and `enabled`.");
        }
        return changes.stream().map(change -> new CapabilityToggle(change.key(), change.enabled())).toList();
    }

    private static boolean incomplete(Change change) {
        return change == null || change.key() == null || change.enabled() == null;
    }
}
