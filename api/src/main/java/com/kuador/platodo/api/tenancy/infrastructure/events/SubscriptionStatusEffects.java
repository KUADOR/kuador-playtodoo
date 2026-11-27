package com.kuador.platodo.api.tenancy.infrastructure.events;

import org.springframework.context.event.EventListener;

import com.kuador.platodo.api.subscriptions.domain.SubscriptionStatus;
import com.kuador.platodo.api.subscriptions.domain.event.SubscriptionStatusChanged;
import com.kuador.platodo.api.tenancy.application.TenantLifecycle;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Applies the effect of a manual subscription status change on the tenant (ADR-014). Listens synchronously, on the
 * thread and inside the transaction of the change, not after the commit: an exception here rolls the subscription
 * change back, so the commercial status and the access to the complex never drift apart. The tenant transition takes
 * the instant of the change, so the suspension date is the date of the subscription status.
 * <ul>
 *   <li>{@code SUSPENDED}: the tenant is suspended and its retention starts.</li>
 *   <li>{@code CANCELLED}: the tenant is archived, keeping the retention of a suspension that was running.</li>
 *   <li>{@code SUSPENDED → ACTIVE}: the tenant is reactivated, only before its purge date.</li>
 *   <li>{@code ACTIVE ⇄ EXPIRING}: no effect.</li>
 * </ul>
 */
public final class SubscriptionStatusEffects {

    private final TenantLifecycle lifecycle;

    public SubscriptionStatusEffects(TenantLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @EventListener
    public void on(SubscriptionStatusChanged changed) {
        TenantId tenant = new TenantId(changed.tenantId().toString());
        switch (changed.to()) {
            case SUSPENDED -> lifecycle.suspend(tenant, changed.at());
            case CANCELLED -> lifecycle.cancel(tenant, changed.at());
            case ACTIVE -> {
                if (changed.from() == SubscriptionStatus.SUSPENDED) {
                    lifecycle.reactivate(tenant, changed.at());
                }
            }
            case EXPIRING -> {
                // Commercial warning only: the complex keeps operating.
            }
        }
    }
}
