package com.kuador.platodo.api.subscriptions.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.kuador.platodo.api.subscriptions.domain.LastPayment;

/** The latest payment of each complex, for the complex list. */
@FunctionalInterface
public interface LastPayments {

    /** The latest payment of every complex among {@code tenantIds} that has one, read in one query (no N+1). */
    Map<UUID, LastPayment> latestOf(Collection<UUID> tenantIds);
}
