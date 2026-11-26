package com.kuador.platodo.api.subscriptions.domain.event;

/** Events of the Plans and subscriptions module (ADR-014). */
public sealed interface SubscriptionEvent permits SubscriptionStatusChanged, SubscriptionPlanChanged {
}
