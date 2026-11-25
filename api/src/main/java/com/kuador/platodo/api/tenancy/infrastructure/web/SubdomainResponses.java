package com.kuador.platodo.api.tenancy.infrastructure.web;

import com.kuador.platodo.api.tenancy.application.CheckSubdomainAvailability.Availability;
import com.kuador.platodo.api.tenancy.application.CheckSubdomainAvailability.Reason;

/** Response bodies of the subdomain operations of the contract. */
final class SubdomainResponses {

    private SubdomainResponses() {
    }

    /** {@code SubdomainSuggestion}. */
    record Suggestion(String subdomain) {
    }

    /** {@code SubdomainAvailability}; {@code reason} is null when available. */
    record AvailabilityResponse(String subdomain, boolean available, Reason reason) {

        static AvailabilityResponse of(Availability availability) {
            return new AvailabilityResponse(availability.subdomain(), availability.available(), availability.reason());
        }
    }

    /** {@code SubdomainChanged}. */
    record Changed(String subdomain, String url) {
    }
}
