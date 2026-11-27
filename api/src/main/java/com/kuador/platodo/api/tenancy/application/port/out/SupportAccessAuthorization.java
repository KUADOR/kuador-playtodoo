package com.kuador.platodo.api.tenancy.application.port.out;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Decides and audits each request of a Kuador platform user on the host of a complex (ADR-011, ADR-020). Implemented
 * by the Audit module, which owns the support sessions and the audit trail. Tenancy only asks before the request runs.
 */
public interface SupportAccessAuthorization {

    /**
     * Records the attempt and tells whether it may run. It is allowed only when the session is open, belongs to the
     * caller and is bound to {@code attempt.tenant()}, the role of the caller still allows the current status of the
     * complex, and the allowed access was recorded. Any failure denies.
     */
    boolean authorize(SupportAccessAttempt attempt);

    /**
     * One request of a platform user on the host of a complex.
     *
     * @param platformUserId account of the caller
     * @param tenant         complex resolved from the host
     * @param sessionHeader  raw value of the support session header, if sent
     * @param httpMethod     method of the request
     * @param routeTemplate  route pattern of the handler, never the concrete path nor the query string
     * @param operationId    operationId of the contract operation; empty when the route is not one
     */
    record SupportAccessAttempt(UUID platformUserId, TenantId tenant, Optional<String> sessionHeader,
            String httpMethod, String routeTemplate, Optional<String> operationId) {

        public SupportAccessAttempt {
            Objects.requireNonNull(platformUserId, "platformUserId");
            Objects.requireNonNull(tenant, "tenant");
            Objects.requireNonNull(sessionHeader, "sessionHeader");
            Objects.requireNonNull(httpMethod, "httpMethod");
            Objects.requireNonNull(routeTemplate, "routeTemplate");
            Objects.requireNonNull(operationId, "operationId");
        }
    }
}
