package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.ComplexRegistration;

/** Schema {@code RegisterComplexRequest}. {@code subdomain} and {@code planId} are required by the contract. */
record RegisterComplexRequest(String displayName, String subdomain, String ownerEmail, UUID planId) {

    /** Longest subdomain the contract accepts in the body; the rules of ADR-012 decide the rest. */
    private static final int MAX_SUBDOMAIN_LENGTH = 255;

    /**
     * @throws InvalidSubscriptionsRequestException if {@code subdomain} or {@code planId} is missing or too long
     * @throws com.kuador.platodo.api.subscriptions.domain.InvalidComplexRegistrationException if a field is invalid
     */
    ComplexRegistration toRegistration() {
        if (subdomain == null || subdomain.length() > MAX_SUBDOMAIN_LENGTH || planId == null) {
            throw new InvalidSubscriptionsRequestException(
                    "`subdomain` (up to %d characters) and `planId` are required.".formatted(MAX_SUBDOMAIN_LENGTH));
        }
        return ComplexRegistration.of(displayName, subdomain, ownerEmail, planId);
    }
}
