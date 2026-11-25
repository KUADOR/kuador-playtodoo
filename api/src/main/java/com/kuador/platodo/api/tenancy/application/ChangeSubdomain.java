package com.kuador.platodo.api.tenancy.application;

import java.time.Clock;
import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.Callers;
import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.TenantEventPublisher;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.SubdomainRules;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;
import com.kuador.platodo.api.tenancy.domain.event.TenantSubdomainChanged;

/**
 * The owner of the complex in {@code TenantScope} changes its subdomain (ADR-012). The new one resolves to the same
 * complex and the previous one stops resolving at once and stays retired for every complex. Only the global directory
 * changes: the schema name does not depend on the subdomain.
 */
public final class ChangeSubdomain {

    private final Callers callers;
    private final SubdomainRules rules;
    private final TenantRegistry registry;
    private final GlobalTransaction transaction;
    private final TenantEventPublisher events;
    private final Clock clock;

    ChangeSubdomain(Callers callers, SubdomainRules rules, TenantRegistry registry, GlobalTransaction transaction,
            TenantEventPublisher events, Clock clock) {
        this.callers = callers;
        this.rules = rules;
        this.registry = registry;
        this.transaction = transaction;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Setting the current subdomain again changes nothing.
     *
     * @return the current subdomain after the change
     * @throws com.kuador.platodo.api.tenancy.domain.SubdomainRejectedException if the subdomain breaks the rules
     * @throws SubdomainTakenException if a complex uses or used the subdomain
     */
    public Subdomain change(String candidate) {
        TenantId id = callers.requireComplexOwner();
        Subdomain next = rules.accept(candidate);
        return transaction.run(() -> {
            RegisteredTenant current = registry.lock(id).orElseThrow(TenantNotFoundException::new);
            if (!current.tenant().is(TenantStatus.ACTIVE)) {
                throw new TenantUnavailableException();
            }
            if (next.equals(current.subdomain())) {
                return next;
            }
            Instant now = clock.instant();
            registry.replaceSubdomain(current, next, now);
            events.publish(new TenantSubdomainChanged(id, current.subdomain().value(), next.value(), now));
            return next;
        });
    }
}
