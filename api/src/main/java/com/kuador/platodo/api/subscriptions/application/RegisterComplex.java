package com.kuador.platodo.api.subscriptions.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.subscriptions.application.port.out.Plans;
import com.kuador.platodo.api.subscriptions.application.port.out.Subscriptions;
import com.kuador.platodo.api.subscriptions.domain.ComplexRegistration;
import com.kuador.platodo.api.subscriptions.domain.PlanNotPublishedException;
import com.kuador.platodo.api.subscriptions.domain.PlanSnapshot;
import com.kuador.platodo.api.subscriptions.domain.Subscription;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantOnboarding;
import com.kuador.platodo.api.tenancy.application.port.in.TenantOnboarding.NewTenant;

/**
 * {@code POST /complexes}: capability {@code COMPLEX_CREATE} (ADR-014, ADR-017). In one global transaction it records
 * the tenant with its subdomain and the {@code ACTIVE} subscription with a snapshot of the published plan, so a
 * rejected subdomain or plan leaves nothing behind and two registrations racing for one subdomain record only one
 * complex. Then Tenancy provisions the schema; if that fails the complex stays {@code PROVISIONING_FAILED}.
 */
public final class RegisterComplex {

    private final CapabilityGuard guard;
    private final Plans plans;
    private final Subscriptions subscriptions;
    private final TenantOnboarding onboarding;
    private final ComplexDetails details;
    private final TransactionOperations globalTransaction;
    private final Clock clock;

    public RegisterComplex(CapabilityGuard guard, Plans plans, Subscriptions subscriptions,
            TenantOnboarding onboarding, ComplexDetails details, TransactionOperations globalTransaction,
            Clock clock) {
        this.guard = guard;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.onboarding = onboarding;
        this.details = details;
        this.globalTransaction = globalTransaction;
        this.clock = clock;
    }

    /**
     * @throws PlanNotPublishedException if the plan does not exist or is a draft
     * @throws com.kuador.platodo.api.tenancy.application.port.in.SubdomainUnavailableException if the subdomain is
     *         invalid, reserved, or is or was used
     */
    public ComplexDetail register(ComplexRegistration registration) {
        PlatformCaller caller = guard.require(Capability.COMPLEX_CREATE);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        TenantId tenant = globalTransaction.execute(status -> {
            PlanSnapshot plan = plans.find(registration.planId())
                    .map(found -> PlanSnapshot.of(found, now))
                    .orElseThrow(PlanNotPublishedException::new);
            TenantId recorded = onboarding.record(new NewTenant(
                    registration.displayName(), registration.subdomain(), registration.ownerEmail()));
            subscriptions.insert(Subscription.start(
                    UUID.randomUUID(), UUID.fromString(recorded.value()), plan, caller.id(), now));
            return recorded;
        });
        onboarding.provision(tenant);
        return details.of(UUID.fromString(tenant.value()));
    }
}
