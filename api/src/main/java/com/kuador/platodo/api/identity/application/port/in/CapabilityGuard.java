package com.kuador.platodo.api.identity.application.port.in;

import java.util.EnumSet;
import java.util.Set;

import com.kuador.platodo.api.identity.application.port.out.OperatorCapabilities;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.PlatformRole;

/**
 * Authorization of the platform operations (ADR-017), for every module that exposes one. It is evaluated on each
 * request: the enabled capabilities are read from the global schema every time, never from the token nor from a
 * cache, so a capability the SUPER_ADMIN disables fails on the operator's next request.
 * <ul>
 *   <li>No accepted token: {@link PlatformCallerNotAuthenticatedException}.</li>
 *   <li>Staff, player, or a platform role this code does not know: {@link PlatformCallerForbiddenException}.</li>
 *   <li>{@link PlatformRole#SUPER_ADMIN}: every operation and capability.</li>
 *   <li>{@link PlatformRole#OPERATOR}: operations open to every platform user and the capabilities enabled for
 *       operators; otherwise {@link CapabilityRevokedException}, or {@link PlatformCallerForbiddenException} for an
 *       operation reserved to the SUPER_ADMIN.</li>
 * </ul>
 */
public final class CapabilityGuard {

    private final OperatorCapabilities operatorCapabilities;

    public CapabilityGuard(OperatorCapabilities operatorCapabilities) {
        this.operatorCapabilities = operatorCapabilities;
    }

    /** Any Kuador team member, for operations both roles may use. */
    public PlatformCaller requirePlatformUser() {
        AuthenticatedPrincipal principal =
                CurrentPrincipal.current().orElseThrow(PlatformCallerNotAuthenticatedException::new);
        if (!principal.is(AccountType.PLATFORM)) {
            throw new PlatformCallerForbiddenException();
        }
        PlatformRole role = PlatformRole.of(principal.role()).orElseThrow(PlatformCallerForbiddenException::new);
        return new PlatformCaller(principal.accountId(), role);
    }

    /** Only the SUPER_ADMIN. */
    public PlatformCaller requireSuperAdmin() {
        PlatformCaller caller = requirePlatformUser();
        if (!caller.isSuperAdmin()) {
            throw new PlatformCallerForbiddenException();
        }
        return caller;
    }

    /** The SUPER_ADMIN, or an operator while {@code capability} is enabled for operators. */
    public PlatformCaller require(Capability capability) {
        PlatformCaller caller = requirePlatformUser();
        if (!caller.isSuperAdmin() && !operatorCapabilities.current().isEnabled(capability)) {
            throw new CapabilityRevokedException(capability);
        }
        return caller;
    }

    /** What {@code caller} may do right now: every capability for the SUPER_ADMIN, the enabled ones for operators. */
    public Set<Capability> effectiveCapabilities(PlatformCaller caller) {
        return caller.isSuperAdmin()
                ? EnumSet.allOf(Capability.class)
                : operatorCapabilities.current().enabledCapabilities();
    }
}
