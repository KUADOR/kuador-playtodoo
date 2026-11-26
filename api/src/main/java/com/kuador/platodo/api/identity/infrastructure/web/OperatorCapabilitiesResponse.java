package com.kuador.platodo.api.identity.infrastructure.web;

import java.util.Arrays;
import java.util.List;

import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.OperatorCapabilitySettings;

/** Schema {@code OperatorCapabilities}: every delegable capability, in declaration order. */
record OperatorCapabilitiesResponse(long version, List<Item> items) {

    record Item(String key, String description, boolean enabled) {
    }

    static OperatorCapabilitiesResponse of(OperatorCapabilitySettings settings) {
        List<Item> items = Arrays.stream(Capability.values())
                .map(capability -> new Item(
                        capability.name(), capability.description(), settings.isEnabled(capability)))
                .toList();
        return new OperatorCapabilitiesResponse(settings.version(), items);
    }
}
