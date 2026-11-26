package com.kuador.platodo.api.identity.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.kuador.platodo.api.identity.domain.CapabilityToggle;

/**
 * The SUPER_ADMIN enabled or disabled capabilities for operators (ADR-017). Published inside the transaction that
 * records the change; consumers listen after the commit.
 *
 * @param capabilities effective changes, never empty
 * @param changedBy    platform user who made them
 * @param occurredAt   when they were made
 */
public record OperatorCapabilitiesChanged(List<CapabilityToggle> capabilities, UUID changedBy, Instant occurredAt) {

    public OperatorCapabilitiesChanged {
        capabilities = List.copyOf(capabilities);
        if (capabilities.isEmpty()) {
            throw new IllegalArgumentException("An event needs at least one change");
        }
        Objects.requireNonNull(changedBy, "changedBy");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
