package com.kuador.platodo.api.audit.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * What the audit keeps of one support request (ADR-011 D12, ADR-020): never the concrete path nor the query string.
 *
 * @param httpMethod    method of the request
 * @param routeTemplate route pattern of the handler, for example {@code /api/v1/staff/bookings/{bookingId}}
 * @param operationId   operationId of the contract operation; empty when the route is not a contract operation
 */
public record AuditedRequest(String httpMethod, String routeTemplate, Optional<String> operationId) {

    public AuditedRequest {
        Objects.requireNonNull(httpMethod, "httpMethod");
        Objects.requireNonNull(routeTemplate, "routeTemplate");
        Objects.requireNonNull(operationId, "operationId");
    }
}
