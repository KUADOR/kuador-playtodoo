package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /tenant} of {@code contracts/tenancy.openapi.yaml}. Only an active complex reaches it: the other states
 * are answered by {@link TenantResolutionFilter}.
 */
@RestController
class TenantStateController {

    @GetMapping(TenancyApiPaths.TENANT_STATE)
    TenantStateResponse state(@RequestAttribute(ActiveTenant.REQUEST_ATTRIBUTE) ActiveTenant tenant) {
        return TenantStateResponse.active(tenant.displayName());
    }
}
