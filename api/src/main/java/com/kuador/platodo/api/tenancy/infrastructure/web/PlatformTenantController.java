package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.net.URI;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.tenancy.application.RegisterTenant;
import com.kuador.platodo.api.tenancy.application.TenancyUseCases;
import com.kuador.platodo.api.tenancy.application.TenantLifecycle;
import com.kuador.platodo.api.tenancy.application.TenantNotFoundException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;

/** Tag «Platform tenants» of {@code contracts/tenancy.openapi.yaml}: registration and lifecycle, global host. */
@RestController
@RequestMapping(TenancyApiPaths.PLATFORM_TENANTS)
class PlatformTenantController {

    private static final String TENANT = "/{tenantId}";

    private final RegisterTenant registerTenant;
    private final TenantLifecycle lifecycle;

    PlatformTenantController(TenancyUseCases useCases) {
        this.registerTenant = useCases.registerTenant();
        this.lifecycle = useCases.lifecycle();
    }

    @PostMapping
    ResponseEntity<TenantResponse> register(@RequestBody RegisterTenantRequest request) {
        RegisteredTenant registered =
                registerTenant.register(request.displayName(), request.subdomain(), request.ownerEmail());
        URI location = URI.create(TenancyApiPaths.PLATFORM_TENANTS + "/" + registered.tenant().id().value());
        return ResponseEntity.created(location).body(TenantResponse.of(registered));
    }

    @PostMapping(TENANT + "/suspend")
    TenantResponse suspend(@PathVariable String tenantId) {
        return change(tenantId, lifecycle::suspend);
    }

    @PostMapping(TENANT + "/activate")
    TenantResponse activate(@PathVariable String tenantId) {
        return change(tenantId, lifecycle::activate);
    }

    @PostMapping(TENANT + "/archive")
    TenantResponse archive(@PathVariable String tenantId) {
        return change(tenantId, lifecycle::archive);
    }

    @PostMapping(TENANT + "/reactivate")
    TenantResponse reactivate(@PathVariable String tenantId) {
        return change(tenantId, lifecycle::reactivate);
    }

    @PutMapping(TENANT + "/retention")
    TenantResponse changeRetention(@PathVariable String tenantId, @RequestBody ChangeRetentionRequest request) {
        return change(tenantId, id -> lifecycle.changeRetention(id, request.days()));
    }

    private static TenantResponse change(String tenantId, Function<TenantId, RegisteredTenant> operation) {
        return TenantResponse.of(operation.apply(tenantIdOf(tenantId)));
    }

    /** A path value that is not a canonical tenant id names no complex: 404, like an unknown id. */
    private static TenantId tenantIdOf(String value) {
        if (!isCanonicalUuid(value)) {
            throw new TenantNotFoundException();
        }
        return new TenantId(value);
    }

    private static boolean isCanonicalUuid(String value) {
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException notAUuid) {
            return false;
        }
    }
}
