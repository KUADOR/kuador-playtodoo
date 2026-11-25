package com.kuador.platodo.api.tenancy.infrastructure.web;

/** {@code ChangeSubdomainRequest} of the contract. */
record ChangeSubdomainRequest(String subdomain) {

    /** @throws InvalidRequestException if the subdomain is missing, blank or too long */
    ChangeSubdomainRequest {
        RequestFields.requireSubdomainCandidate(subdomain);
    }
}
