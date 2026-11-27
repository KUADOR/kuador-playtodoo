package com.kuador.platodo.api.tenancy.application;

import java.time.Clock;
import java.time.Instant;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantRetention;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;
import com.kuador.platodo.api.tenancy.domain.Tenant;
import com.kuador.platodo.api.tenancy.domain.event.TenantActivated;
import com.kuador.platodo.api.tenancy.domain.event.TenantArchived;
import com.kuador.platodo.api.tenancy.domain.event.TenantSuspended;

/**
 * Lifecycle effects of the subscription of a complex (ADR-014): Kuador changes the subscription and Tenancy applies
 * the effect on the tenant inside the same global transaction. Authorizing the caller is the job of the subscription
 * operation that triggers them. A transition the current state does not allow throws
 * {@link com.kuador.platodo.api.tenancy.domain.InvalidTransitionException}.
 */
public final class TenantLifecycle implements TenantRetention {

    private final TenantTransitions transitions;
    private final Clock clock;
    private final int defaultRetentionDays;

    TenantLifecycle(TenantTransitions transitions, Clock clock, int defaultRetentionDays) {
        this.transitions = transitions;
        this.clock = clock;
        this.defaultRetentionDays = defaultRetentionDays;
    }

    /** {@code ACTIVE → SUSPENDED} at {@code at}, with the default retention of the environment. */
    public RegisteredTenant suspend(TenantId id, Instant at) {
        return transitions.apply(id, tenant -> tenant.suspend(at, defaultRetentionDays),
                changed -> new TenantSuspended(id, changed.tenant().suspendedAt()));
    }

    /**
     * {@code ACTIVE | SUSPENDED → ARCHIVED} at {@code at}: an active complex gets the default retention of the
     * environment; a suspended one keeps the purge date that was running.
     */
    public RegisteredTenant cancel(TenantId id, Instant at) {
        return transitions.apply(id, tenant -> tenant.archive(at, defaultRetentionDays),
                changed -> new TenantArchived(id, changed.tenant().purgeDueAt(), changed.tenant().archivedAt()));
    }

    /**
     * {@code SUSPENDED → ACTIVE} at {@code at}, before the purge date.
     *
     * @throws com.kuador.platodo.api.tenancy.domain.RetentionExpiredException if the purge date was reached
     */
    public RegisteredTenant reactivate(TenantId id, Instant at) {
        return transitions.apply(id, tenant -> tenant.reactivate(at), TenantLifecycle::activated);
    }

    /** Publishes no event: the complex keeps its status, only its purge date moves. */
    @Override
    public void changeRetention(TenantId id, int days) {
        transitions.applyWithoutEvent(id, tenant -> tenant.changeRetention(days, clock.instant()));
    }

    /** Activation of a complex whose schema and owner exist; used by registration. */
    RegisteredTenant activateProvisioned(TenantId id) {
        return transitions.apply(id, tenant -> tenant.activate(clock.instant()), TenantLifecycle::activated);
    }

    private static TenantActivated activated(RegisteredTenant changed) {
        Tenant tenant = changed.tenant();
        return new TenantActivated(tenant.id(), changed.subdomain().value(), tenant.activatedAt());
    }
}
