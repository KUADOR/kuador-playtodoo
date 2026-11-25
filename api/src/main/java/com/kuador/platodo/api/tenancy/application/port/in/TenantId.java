package com.kuador.platodo.api.tenancy.application.port.in;

import java.util.Objects;

/** Identifier of a tenant (one sports complex), as Tenancy knows it. */
public record TenantId(String value) {

    public TenantId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A tenant id cannot be blank");
        }
    }
}
