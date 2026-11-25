package com.kuador.platodo.api.tenancy.application.port.in;

import java.util.function.Consumer;

/**
 * Lets background work (scheduled jobs) run inside every active tenant without knowing how schemas
 * are resolved. Request-scoped work does not use it: Tenancy resolves that tenant by subdomain.
 */
public interface ActiveTenantRunner {

    /**
     * Runs {@code work} once per active tenant, one tenant at a time. While {@code work} runs, data access on
     * the current thread uses only that tenant's schema. Suspended tenants are skipped.
     * An exception thrown by {@code work} propagates and stops the iteration.
     */
    void forEachActiveTenant(Consumer<TenantId> work);
}
