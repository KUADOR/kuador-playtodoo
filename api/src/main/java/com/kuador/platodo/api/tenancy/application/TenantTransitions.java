package com.kuador.platodo.api.tenancy.application;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.TenantEventPublisher;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry.RegisteredTenant;
import com.kuador.platodo.api.tenancy.domain.Tenant;
import com.kuador.platodo.api.tenancy.domain.event.TenantEvent;

/**
 * Applies one lifecycle transition atomically: locks the complex, checks the transition against its current state,
 * stores it and publishes its event in the same global transaction. Two concurrent transitions of one complex run one
 * after the other, so the second one sees the state the first one left.
 */
final class TenantTransitions {

    private final TenantRegistry registry;
    private final GlobalTransaction transaction;
    private final TenantEventPublisher events;

    TenantTransitions(TenantRegistry registry, GlobalTransaction transaction, TenantEventPublisher events) {
        this.registry = registry;
        this.transaction = transaction;
        this.events = events;
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

    /** Like {@link #apply}, for a change that publishes no event. */
    RegisteredTenant applyWithoutEvent(TenantId id, UnaryOperator<Tenant> change) {
        return transaction.run(() -> lockAndSave(id, change));
    }

    private RegisteredTenant lockAndSave(TenantId id, UnaryOperator<Tenant> change) {
        RegisteredTenant current = registry.lock(id).orElseThrow(TenantNotFoundException::new);
        RegisteredTenant changed = new RegisteredTenant(change.apply(current.tenant()), current.subdomain());
        registry.save(changed.tenant());
        return changed;
    }
}
