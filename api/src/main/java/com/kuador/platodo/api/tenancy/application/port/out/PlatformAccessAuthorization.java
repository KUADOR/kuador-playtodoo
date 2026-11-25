package com.kuador.platodo.api.tenancy.application.port.out;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Decides and audits each request of a Kuador platform user on the host of a complex (ADR-011). Implemented by the
 * Audit module, which owns the access sessions and the audit trail. Tenancy only asks before the request runs.
 */
public interface PlatformAccessAuthorization {

    /**
     * Records the attempt and tells whether it may run. It is allowed only when the session is open, belongs to the
     * caller and is bound to {@code attempt.tenant()}, the caller is the platform owner, and the allowed access was
     * recorded. Any failure denies.
     */
    boolean authorize(PlatformAccessAttempt attempt);

    /**
     * One request of a platform user on the host of a complex.
     *
     * @param platformUserId account of the caller
     * @param platformOwner  whether the caller has the only role allowed to reach complex data
     * @param tenant         complex resolved from the host
     * @param sessionHeader  raw value of the access session header, if sent
     * @param httpMethod     method of the request
     * @param routeTemplate  route pattern of the handler, never the concrete path nor the query string
     */
    record PlatformAccessAttempt(UUID platformUserId, boolean platformOwner, TenantId tenant,
            Optional<String> sessionHeader, String httpMethod, String routeTemplate) {

        public PlatformAccessAttempt {
            Objects.requireNonNull(platformUserId, "platformUserId");
            Objects.requireNonNull(tenant, "tenant");
            Objects.requireNonNull(sessionHeader, "sessionHeader");
            Objects.requireNonNull(httpMethod, "httpMethod");
            Objects.requireNonNull(routeTemplate, "routeTemplate");
        }
    }
}
