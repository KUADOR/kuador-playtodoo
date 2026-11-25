package com.kuador.platodo.api.tenancy.application;

import java.time.Clock;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.Callers;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;
import com.kuador.platodo.api.tenancy.domain.Tenant;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;
import com.kuador.platodo.api.tenancy.domain.event.TenantActivated;
import com.kuador.platodo.api.tenancy.domain.event.TenantArchived;
import com.kuador.platodo.api.tenancy.domain.event.TenantSuspended;

/**
 * Lifecycle operations of Kuador on a complex (ADR-013), only for the platform owner. A transition the current state
 * does not allow throws {@link com.kuador.platodo.api.tenancy.domain.InvalidTransitionException}.
 */
public final class TenantLifecycle {

    private final Callers callers;
    private final TenantRegistry registry;
    private final TenantTransitions transitions;
    private final TenantProvisioning provisioning;
    private final Clock clock;
    private final int defaultRetentionDays;

    TenantLifecycle(Callers callers, TenantRegistry registry, TenantTransitions transitions,
            TenantProvisioning provisioning, Clock clock, int defaultRetentionDays) {
        this.callers = callers;
        this.registry = registry;
        this.transitions = transitions;
        this.provisioning = provisioning;
        this.clock = clock;
        this.defaultRetentionDays = defaultRetentionDays;
    }

    public RegisteredTenant suspend(TenantId id) {
        callers.requirePlatformOwner();
        return transitions.apply(id, tenant -> tenant.suspend(clock.instant()),
                changed -> new TenantSuspended(id, changed.tenant().suspendedAt()));
    }

    /** {@code SUSPENDED → ACTIVE}, or {@code PROVISIONING → ACTIVE} retrying the provisioning first. */
    public RegisteredTenant activate(TenantId id) {
        callers.requirePlatformOwner();
        boolean provisioningPending = registry.find(id)
                .orElseThrow(TenantNotFoundException::new)
                .tenant().is(TenantStatus.PROVISIONING);
        if (provisioningPending) {
            provisioning.retry(id);
        }
        return activateProvisioned(id);
    }

    /** Archives with the default retention of the environment ({@code tenancy.archive.default-retention-days}). */
    public RegisteredTenant archive(TenantId id) {
        callers.requirePlatformOwner();
        return transitions.apply(id, tenant -> tenant.archive(clock.instant(), defaultRetentionDays),
                changed -> new TenantArchived(id, changed.tenant().purgeDueAt(), changed.tenant().archivedAt()));
    }

    public RegisteredTenant reactivate(TenantId id) {
        callers.requirePlatformOwner();
        return transitions.apply(id, tenant -> tenant.reactivate(clock.instant()), TenantLifecycle::activated);
    }

    /** Publishes no event: the complex stays archived, only its purge date moves. */
    public RegisteredTenant changeRetention(TenantId id, int days) {
        callers.requirePlatformOwner();
        return transitions.applyWithoutEvent(id, tenant -> tenant.changeRetention(days, clock.instant()));
    }

    /** Activation of a complex whose schema and owner exist; used by registration as well. */
    RegisteredTenant activateProvisioned(TenantId id) {
        return transitions.apply(id, tenant -> tenant.activate(clock.instant()), TenantLifecycle::activated);
    }

    private static TenantActivated activated(RegisteredTenant changed) {
        Tenant tenant = changed.tenant();
        return new TenantActivated(tenant.id(), changed.subdomain().value(), tenant.activatedAt());
    }
}
