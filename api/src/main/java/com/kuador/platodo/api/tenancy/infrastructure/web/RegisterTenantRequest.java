package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.util.regex.Pattern;

import com.kuador.platodo.api.tenancy.domain.Tenant;

/** {@code RegisterTenantRequest} of the contract. */
record RegisterTenantRequest(String displayName, String subdomain, String ownerEmail) {

    private static final int MAX_EMAIL_LENGTH = 254;
    /** One {@code @} between a non-empty local part and a non-empty domain, without spaces. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    /** @throws InvalidRequestException if a field is missing, blank, too long or the e-mail is malformed */
    RegisterTenantRequest {
        RequestFields.requireText(displayName, "displayName", Tenant.MAX_DISPLAY_NAME_LENGTH);
        RequestFields.requireSubdomainCandidate(subdomain);
        RequestFields.requireText(ownerEmail, "ownerEmail", MAX_EMAIL_LENGTH);
        if (!EMAIL.matcher(ownerEmail.strip()).matches()) {
            throw new InvalidRequestException("ownerEmail is not an e-mail address");
        }
    }
}
