package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.tenancy.application.RegisterTenant;
import com.kuador.platodo.api.tenancy.application.TenancyUseCases;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;

/**
 * Tag «Platform tenants» of {@code contracts/tenancy.openapi.yaml}: registration, global host. The lifecycle is changed
 * only through the subscription of the complex (ADR-014), so its direct operations are gone.
 */
@RestController
@RequestMapping(TenancyApiPaths.PLATFORM_TENANTS)
class PlatformTenantController {

    private final RegisterTenant registerTenant;

    PlatformTenantController(TenancyUseCases useCases) {
        this.registerTenant = useCases.registerTenant();
    }

    @PostMapping
    ResponseEntity<TenantResponse> register(@RequestBody RegisterTenantRequest request) {
        RegisteredTenant registered =
                registerTenant.register(request.displayName(), request.subdomain(), request.ownerEmail());
        URI location = URI.create(TenancyApiPaths.PLATFORM_TENANTS + "/" + registered.tenant().id().value());
        return ResponseEntity.created(location).body(TenantResponse.of(registered));
    }
}
