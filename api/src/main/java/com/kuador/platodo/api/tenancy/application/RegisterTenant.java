package com.kuador.platodo.api.tenancy.application;

import java.time.Clock;
import java.util.UUID;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.Callers;
import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.SubdomainRules;
import com.kuador.platodo.api.tenancy.domain.Tenant;

/**
 * Registration of a complex by Kuador: records it as {@code PROVISIONING} with its subdomain, provisions its schema
 * and owner account, and activates it. If provisioning fails the complex stays {@code PROVISIONING} and
 * {@link TenantLifecycle#activate} retries it.
 */
public final class RegisterTenant {

    private final Callers callers;
    private final SubdomainRules rules;
    private final TenantRegistry registry;
    private final GlobalTransaction transaction;
    private final TenantProvisioning provisioning;
    private final TenantLifecycle lifecycle;
    private final Clock clock;

    RegisterTenant(Callers callers, SubdomainRules rules, TenantRegistry registry, GlobalTransaction transaction,
            TenantProvisioning provisioning, TenantLifecycle lifecycle, Clock clock) {
        this.callers = callers;
        this.rules = rules;
        this.registry = registry;
        this.transaction = transaction;
        this.provisioning = provisioning;
        this.lifecycle = lifecycle;
        this.clock = clock;
    }

    /**
     * @throws com.kuador.platodo.api.tenancy.domain.SubdomainRejectedException if the subdomain breaks the rules
     * @throws SubdomainTakenException if a complex uses or used the subdomain; nothing is recorded then
     */
    public RegisteredTenant register(String displayName, String subdomain, String ownerEmail) {
        callers.requirePlatformOwner();
        Subdomain accepted = rules.accept(subdomain);
        Tenant tenant = Tenant.provisioning(new TenantId(UUID.randomUUID().toString()), displayName, clock.instant());
        transaction.run(() -> {
            registry.register(tenant, accepted, ownerEmail);
            return tenant;
        });
        provisioning.provision(tenant.id(), ownerEmail);
        return lifecycle.activateProvisioned(tenant.id());
    }
}
