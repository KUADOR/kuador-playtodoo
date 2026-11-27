package com.kuador.platodo.api.tenancy.application;

import java.time.Clock;
import java.time.Duration;

import com.kuador.platodo.api.tenancy.application.port.in.TenantOnboarding;
import com.kuador.platodo.api.tenancy.application.port.out.Callers;
import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.SubdomainUsage;
import com.kuador.platodo.api.tenancy.application.port.out.TenantEventPublisher;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerAccounts;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantSchemas;
import com.kuador.platodo.api.tenancy.domain.SubdomainRules;

/**
 * The lifecycle and subdomain use cases of Tenancy, built together because they share the transition and
 * provisioning steps, which stay internal to this package.
 */
public record TenancyUseCases(
        RegisterTenant registerTenant,
        TenantLifecycle lifecycle,
        ChangeSubdomain changeSubdomain,
        SuggestSubdomain suggestSubdomain,
        CheckSubdomainAvailability checkSubdomainAvailability,
        TenantOnboarding onboarding) {

    /** The adapters the use cases need. */
    public record Ports(
            Callers callers,
            TenantRegistry registry,
            SubdomainUsage subdomainUsage,
            TenantSchemas schemas,
            TenantOwnerAccounts owners,
            TenantEventPublisher events,
            GlobalTransaction transaction) {
    }

    /** PostgreSQL keeps {@code timestamptz} in microseconds. */
    private static final Duration DATABASE_PRECISION = Duration.ofNanos(1_000);

    /**
     * @param clock                any clock; the use cases read it at the precision of the database, so the times
     *                             they answer are exactly the stored ones
     * @param defaultRetentionDays retention of a new suspension or archive
     *                             ({@code tenancy.archive.default-retention-days})
     */
    public static TenancyUseCases create(Ports ports, SubdomainRules rules, Clock clock, int defaultRetentionDays) {
        return build(ports, rules, atDatabasePrecision(clock), defaultRetentionDays);
    }

    static Clock atDatabasePrecision(Clock clock) {
        return Clock.tick(clock, DATABASE_PRECISION);
    }

    private static TenancyUseCases build(Ports ports, SubdomainRules rules, Clock clock, int defaultRetentionDays) {
        TenantTransitions transitions = new TenantTransitions(ports.registry(), ports.transaction(), ports.events(),
                clock);
        TenantProvisioning provisioning = new TenantProvisioning(ports.schemas(), ports.owners());
        TenantLifecycle lifecycle = new TenantLifecycle(transitions, clock, defaultRetentionDays);
        return new TenancyUseCases(
                new RegisterTenant(ports.callers(), rules, ports.registry(), ports.transaction(), provisioning,
                        lifecycle, clock),
                lifecycle,
                new ChangeSubdomain(ports.callers(), rules, ports.registry(), ports.transaction(), ports.events(),
                        clock),
                new SuggestSubdomain(rules, ports.subdomainUsage()),
                new CheckSubdomainAvailability(rules, ports.subdomainUsage()),
                new OnboardTenant(rules, ports.registry(), ports.transaction(), provisioning, transitions, lifecycle,
                        clock));
    }
}
