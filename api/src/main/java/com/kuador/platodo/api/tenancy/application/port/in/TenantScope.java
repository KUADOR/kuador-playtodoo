package com.kuador.platodo.api.tenancy.application.port.in;

import java.util.Objects;
import java.util.Optional;

/**
 * The tenant whose schema the current work may use (ADR-010). Tenant persistence reads it on every connection and
 * fails closed when no scope is open: there is no default tenant.
 *
 * <p>Built on {@link ScopedValue}: the scope ends with the operation and is not inherited by other threads, so
 * asynchronous listeners and jobs receive the {@link TenantId} explicitly and open their own scope. A scope for
 * another tenant cannot be opened inside an open one: one unit of work never touches two tenant schemas.
 */
public final class TenantScope {

    private static final ScopedValue<TenantId> CURRENT = ScopedValue.newInstance();

    private TenantScope() {
    }

    /** Runs {@code operation} inside the scope of {@code tenant}. */
    public static void run(TenantId tenant, Runnable operation) {
        bind(tenant).run(operation);
    }

    /** Calls {@code operation} inside the scope of {@code tenant} and returns its result. */
    public static <T, X extends Throwable> T call(TenantId tenant, ScopedValue.CallableOp<? extends T, X> operation)
            throws X {
        return bind(tenant).call(operation);
    }

    /** The tenant of the open scope, if any. */
    public static Optional<TenantId> current() {
        return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
    }

    /** The tenant of the open scope; fails closed with {@link NoTenantInScopeException} when there is none. */
    public static TenantId require() {
        if (!CURRENT.isBound()) {
            throw new NoTenantInScopeException();
        }
        return CURRENT.get();
    }

    private static ScopedValue.Carrier bind(TenantId tenant) {
        Objects.requireNonNull(tenant, "tenant");
        if (CURRENT.isBound() && !CURRENT.get().equals(tenant)) {
            throw new IllegalStateException("A tenant scope is already open for another tenant");
        }
        return ScopedValue.where(CURRENT, tenant);
    }
}
