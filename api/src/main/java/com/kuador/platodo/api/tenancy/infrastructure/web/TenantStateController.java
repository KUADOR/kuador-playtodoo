package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /tenant} of {@code contracts/tenancy.openapi.yaml}: answered only for an active complex. The other states
 * are answered by {@link TenantResolutionFilter}; a suspended or archived complex reached by Kuador support answers
 * {@code TENANT_UNAVAILABLE} here too, because this public state never reports anything but {@code ACTIVE}.
 */
@RestController
class TenantStateController {

    @GetMapping(TenancyApiPaths.TENANT_STATE)
    TenantStateResponse state(@RequestAttribute(ResolvedTenant.REQUEST_ATTRIBUTE) ResolvedTenant tenant) {
        if (!tenant.isActive()) {
            throw new TenantRequestRejectedException(TenancyProblem.TENANT_UNAVAILABLE);
        }
        return TenantStateResponse.active(tenant.displayName());
    }
}
