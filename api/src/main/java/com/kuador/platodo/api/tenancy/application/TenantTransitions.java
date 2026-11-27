package com.kuador.platodo.api.tenancy.application;

import java.time.Clock;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.TenantEventPublisher;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;
import com.kuador.platodo.api.tenancy.domain.Tenant;
import com.kuador.platodo.api.tenancy.domain.event.TenantEvent;
import com.kuador.platodo.api.tenancy.domain.event.TenantStatusChanged;

/**
 * Applies one lifecycle transition atomically: locks the complex, checks the transition against its current state,
 * stores it and publishes its events in the same global transaction, joining the one of the caller if there is one.
 * Every change of status also publishes {@link TenantStatusChanged}, whose synchronous consumers commit or roll back
 * with it (ADR-014). Two concurrent transitions of one complex run one after the other, so the second one sees the
 * state the first one left.
 */
final class TenantTransitions {

    private final TenantRegistry registry;
    private final GlobalTransaction transaction;
    private final TenantEventPublisher events;
    private final Clock clock;

    TenantTransitions(TenantRegistry registry, GlobalTransaction transaction, TenantEventPublisher events,
            Clock clock) {
        this.registry = registry;
        this.transaction = transaction;
        this.events = events;
        this.clock = clock;
    }

    /**
     * @param change the transition; it throws when the current state does not allow it
     * @param event  the event of the changed complex
     */
    RegisteredTenant apply(TenantId id, UnaryOperator<Tenant> change, Function<RegisteredTenant, TenantEvent> event) {
        return transaction.run(() -> {
            RegisteredTenant changed = lockAndSave(id, change);
            events.publish(event.apply(changed));
            return changed;
        });
    }

    /** Like {@link #apply}, for a change without an event of its own; a change of status still publishes one. */
    RegisteredTenant applyWithoutEvent(TenantId id, UnaryOperator<Tenant> change) {
        return transaction.run(() -> lockAndSave(id, change));
    }

    private RegisteredTenant lockAndSave(TenantId id, UnaryOperator<Tenant> change) {
        RegisteredTenant current = registry.lock(id).orElseThrow(TenantNotFoundException::new);
        RegisteredTenant changed = new RegisteredTenant(change.apply(current.tenant()), current.subdomain());
        registry.save(changed.tenant());
        Tenant tenant = changed.tenant();
        if (tenant.status() != current.tenant().status()) {
            events.publish(new TenantStatusChanged(id, tenant.status(), tenant.purgeDueAt(), clock.instant()));
        }
        return changed;
    }
}
