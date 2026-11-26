package com.kuador.platodo.api.identity.domain;

import java.util.Arrays;
import java.util.Optional;

/** Role of a Kuador team member (claim {@code role} of a {@link AccountType#PLATFORM} token, ADR-017). */
public enum PlatformRole {

    /** Owner of Kuador: every platform operation and every capability. */
    SUPER_ADMIN,
    /** Reads complexes and does what the {@link #SUPER_ADMIN} enabled among the delegable capabilities. */
    OPERATOR;

    /** The role named {@code value}; empty for any other value, such as a role that no longer exists. */
    public static Optional<PlatformRole> of(String value) {
        return Arrays.stream(values()).filter(role -> role.name().equals(value)).findFirst();
    }
}
