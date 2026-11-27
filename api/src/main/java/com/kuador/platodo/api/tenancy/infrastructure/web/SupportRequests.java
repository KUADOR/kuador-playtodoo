package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsUtils;

import jakarta.servlet.http.HttpServletRequest;

/** How a request on the host of a complex carries a support session of Kuador (ADR-020). */
final class SupportRequests {

    /** Header with the id of the support session opened on the global host. */
    static final String SESSION_HEADER = "X-Support-Session";

    private SupportRequests() {
    }

    /** The raw value of the support session header, if sent. */
    static Optional<String> sessionHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(SESSION_HEADER));
    }

    /**
     * Whether the request sends a support session, or is the CORS preflight of one, which carries no value yet. It
     * only says the request claims to be support: {@link SupportAccessGuard} decides whether it is.
     */
    static boolean announceSession(HttpServletRequest request) {
        return sessionHeader(request).isPresent() || isPreflightOfSession(request);
    }

    private static boolean isPreflightOfSession(HttpServletRequest request) {
        String requestedHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        return CorsUtils.isPreFlightRequest(request) && requestedHeaders != null
                && Arrays.stream(requestedHeaders.split(","))
                        .anyMatch(header -> header.strip().equalsIgnoreCase(SESSION_HEADER));
    }
}
