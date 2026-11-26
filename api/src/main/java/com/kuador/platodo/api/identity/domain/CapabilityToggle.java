package com.kuador.platodo.api.identity.domain;

import java.util.Objects;

/** Enabling ({@code enabled = true}) or disabling one capability for operators. */
public record CapabilityToggle(Capability capability, boolean enabled) {

    public CapabilityToggle {
        Objects.requireNonNull(capability, "capability");
    }
}
