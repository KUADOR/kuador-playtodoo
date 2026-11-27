package com.kuador.platodo.api.audit.support;

import java.util.UUID;

import com.kuador.platodo.api.audit.application.CurrentSupportCaller;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.support.InMemoryOperatorCapabilities;

/** The real {@link CapabilityGuard} over in-memory operator capabilities, and helpers to act as a platform user. */
public final class SupportCallers {

    private final InMemoryOperatorCapabilities capabilities = new InMemoryOperatorCapabilities();
    private final CapabilityGuard guard = new CapabilityGuard(capabilities);

    public CapabilityGuard guard() {
        return guard;
    }

    public CurrentSupportCaller current() {
        return new CurrentSupportCaller(guard);
    }

    /** Enables or disables {@code TENANT_DATA_ACCESS} for operators, as the SUPER_ADMIN would. */
    public void tenantDataAccessForOperators(boolean enabled) {
        capabilities.set(Capability.TENANT_DATA_ACCESS, enabled);
    }

    public static AuthenticatedPrincipal superAdmin(UUID id) {
        return AuthenticatedPrincipal.global(id, AccountType.PLATFORM, "SUPER_ADMIN");
    }

    public static AuthenticatedPrincipal operator(UUID id) {
        return AuthenticatedPrincipal.global(id, AccountType.PLATFORM, "OPERATOR");
    }

    /** Runs {@code operation} as {@code principal}. */
    public static <T> T as(AuthenticatedPrincipal principal, ScopedValue.CallableOp<T, RuntimeException> operation) {
        return CurrentPrincipal.call(principal, operation);
    }
}
