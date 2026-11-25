package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Name of a tenant schema: {@code t_} followed by the tenant UUID without hyphens (ADR-010, D4). It is immutable,
 * never derived from the subdomain nor from user input, and always matches {@code ^t_[0-9a-f]{32}$}, which makes it
 * safe to write into SQL as an identifier.
 */
public record TenantSchemaName(String value) {

    private static final Pattern FORMAT = Pattern.compile("^t_[0-9a-f]{32}$");
    private static final String PREFIX = "t_";
    private static final String NOT_A_CANONICAL_UUID = "A tenant id must be a canonical lowercase UUID";

    public TenantSchemaName {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Not a tenant schema name: " + value);
        }
    }

    /** The schema of {@code tenant}, whose id must be a canonical lowercase UUID. */
    public static TenantSchemaName of(TenantId tenant) {
        return new TenantSchemaName(PREFIX + canonicalUuid(tenant).toString().replace("-", ""));
    }

    private static UUID canonicalUuid(TenantId tenant) {
        UUID id;
        try {
            id = UUID.fromString(tenant.value());
        } catch (IllegalArgumentException notAUuid) {
            throw new IllegalArgumentException(NOT_A_CANONICAL_UUID, notAUuid);
        }
        if (!id.toString().equals(tenant.value())) {
            throw new IllegalArgumentException(NOT_A_CANONICAL_UUID);
        }
        return id;
    }
}
