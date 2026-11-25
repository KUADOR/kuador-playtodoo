package com.kuador.platodo.api.tenancy.infrastructure.web;

/** {@code TenantState} of the contract: always {@code ACTIVE}, any other state is answered with a problem. */
record TenantStateResponse(String status, String displayName) {

    private static final String ACTIVE = "ACTIVE";

    static TenantStateResponse active(String displayName) {
        return new TenantStateResponse(ACTIVE, displayName);
    }
}
