package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.tenancy.application.ChangeSubdomain;
import com.kuador.platodo.api.tenancy.application.TenancyUseCases;

/** {@code PUT /tenant/subdomain} of {@code contracts/tenancy.openapi.yaml}: the owner, on the host of the complex. */
@RestController
class TenantSubdomainController {

    private final ChangeSubdomain changeSubdomain;
    private final TenantUrls urls;

    TenantSubdomainController(TenancyUseCases useCases, TenantUrls urls) {
        this.changeSubdomain = useCases.changeSubdomain();
        this.urls = urls;
    }

    @PutMapping(TenancyApiPaths.TENANT_SUBDOMAIN)
    SubdomainResponses.Changed change(@RequestBody ChangeSubdomainRequest request) {
        String subdomain = changeSubdomain.change(request.subdomain()).value();
        return new SubdomainResponses.Changed(subdomain, urls.of(subdomain));
    }
}
