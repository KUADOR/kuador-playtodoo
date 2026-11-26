package com.kuador.platodo.api.tenancy.application;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kuador.platodo.api.tenancy.application.port.in.ProvisioningNotFailedException;
import com.kuador.platodo.api.tenancy.application.port.in.SubdomainUnavailableException;
import com.kuador.platodo.api.tenancy.application.port.in.SubdomainUnavailableException.Reason;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantOnboarding;
import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.domain.InvalidTransitionException;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.SubdomainRejectedException;
import com.kuador.platodo.api.tenancy.domain.SubdomainRules;
import com.kuador.platodo.api.tenancy.domain.Tenant;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * {@link TenantOnboarding} with the registry, provisioning and transitions of the lifecycle. A failed provisioning is
 * resolved here: it is logged and recorded as {@code PROVISIONING_FAILED}, so the backoffice can retry it.
 */
public final class OnboardTenant implements TenantOnboarding {

    private static final Logger log = LoggerFactory.getLogger(OnboardTenant.class);

    private final SubdomainRules rules;
    private final TenantRegistry registry;
    private final GlobalTransaction transaction;
    private final TenantProvisioning provisioning;
    private final TenantTransitions transitions;
    private final TenantLifecycle lifecycle;
    private final Clock clock;

    OnboardTenant(SubdomainRules rules, TenantRegistry registry, GlobalTransaction transaction,
            TenantProvisioning provisioning, TenantTransitions transitions, TenantLifecycle lifecycle, Clock clock) {
        this.rules = rules;
        this.registry = registry;
        this.transaction = transaction;
        this.provisioning = provisioning;
        this.transitions = transitions;
        this.lifecycle = lifecycle;
        this.clock = clock;
    }

    @Override
    public TenantId record(NewTenant newTenant) {
        Subdomain subdomain = acceptedSubdomain(newTenant.subdomain());
        Tenant tenant = Tenant.provisioning(
                new TenantId(UUID.randomUUID().toString()), newTenant.displayName(), clock.instant());
        try {
            transaction.run(() -> {
                registry.register(tenant, subdomain, newTenant.ownerEmail());
                return tenant;
            });
        } catch (SubdomainTakenException taken) {
            throw new SubdomainUnavailableException(Reason.TAKEN);
        }
        return tenant.id();
    }

    @Override
    public TenantStatus provision(TenantId tenant) {
        String ownerEmail = registry.ownerEmailOf(tenant).orElseThrow(() -> new IllegalStateException(
                "Tenant " + tenant.value() + " was not recorded through the onboarding"));
        try {
            provisioning.provision(tenant, ownerEmail);
        } catch (RuntimeException failure) {
            log.warn("Provisioning of tenant {} failed; it waits for a retry", tenant.value(), failure);
            return transitions.applyWithoutEvent(tenant, Tenant::failProvisioning).tenant().status();
        }
        return lifecycle.activateProvisioned(tenant).tenant().status();
    }

    @Override
    public TenantStatus retryProvisioning(TenantId tenant) {
        try {
            transitions.applyWithoutEvent(tenant, Tenant::retryProvisioning);
        } catch (TenantNotFoundException | InvalidTransitionException notFailed) {
            throw new ProvisioningNotFailedException();
        }
        return provision(tenant);
    }

    private Subdomain acceptedSubdomain(String candidate) {
        try {
            return rules.accept(candidate);
        } catch (SubdomainRejectedException rejected) {
            throw new SubdomainUnavailableException(Reason.valueOf(rejected.rejection().name()));
        }
    }
}
