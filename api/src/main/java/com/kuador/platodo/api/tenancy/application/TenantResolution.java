package com.kuador.platodo.api.tenancy.application;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** Outcome of resolving the complex of a tenant host. */
public sealed interface TenantResolution {

    /** The complex is active: the request runs inside its schema. */
    record Active(TenantId tenantId, String displayName) implements TenantResolution {
    }

    /**
     * The complex exists but is suspended or archived. Which one is never disclosed to the caller; only Kuador
     * support may still work on it (ADR-020).
     */
    record Unavailable(TenantId tenantId, String displayName, TenantStatus status) implements TenantResolution {
    }

    /** Malformed, unknown or retired subdomain, or a complex that is not (or no longer) operating. */
    record NotFound() implements TenantResolution {
    }
}
