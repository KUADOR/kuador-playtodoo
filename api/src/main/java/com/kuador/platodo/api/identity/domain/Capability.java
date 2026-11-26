package com.kuador.platodo.api.identity.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Capabilities the {@link PlatformRole#SUPER_ADMIN} can enable for operators (ADR-017). They are defined here, in
 * code; only whether operators have them is data. A new capability is added here, guarded where it applies and seeded
 * disabled by a migration of {@code platform_capability} and {@code operator_capability}.
 */
public enum Capability {

    COMPLEX_CREATE("Register complexes and retry their provisioning."),
    SUBSCRIPTION_PAYMENT_RECORD("Record subscription payments."),
    SUBSCRIPTION_STATUS_CHANGE("Mark expiring, suspend, reactivate and cancel subscriptions."),
    TENANT_DATA_ACCESS("Open support sessions with a reason on active complexes to read and edit their data.");

    private final String description;

    Capability(String description) {
        this.description = description;
    }

    /** Developer-facing description; the backoffice shows its own Spanish text by key. */
    public String description() {
        return description;
    }

    /** The capability stored as {@code key}; empty for a key this version of the code does not know. */
    public static Optional<Capability> ofKey(String key) {
        return Arrays.stream(values()).filter(capability -> capability.name().equals(key)).findFirst();
    }
}
