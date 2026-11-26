package com.kuador.platodo.api.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.CapabilityRevokedException;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.application.port.in.PlatformCaller;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerNotAuthenticatedException;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.PlatformRole;
import com.kuador.platodo.api.identity.support.InMemoryOperatorCapabilities;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** ADR-017 authorization rules, evaluated on every call (PLATODO-82, CA15 and CA16). */
class CapabilityGuardTest {

    private static final Capability PAYMENTS = Capability.SUBSCRIPTION_PAYMENT_RECORD;
    private static final TenantId TENANT = new TenantId(UUID.randomUUID().toString());

    private final InMemoryOperatorCapabilities capabilities = new InMemoryOperatorCapabilities();
    private final CapabilityGuard guard = new CapabilityGuard(capabilities);

    @Test
    void withoutAPrincipalNoPlatformOperationIsAllowed() {
        assertThatThrownBy(guard::requirePlatformUser).isInstanceOf(PlatformCallerNotAuthenticatedException.class);
    }

    @Test
    void staffAndPlayersAreForbiddenEvenWithAPlatformRoleName() {
        AuthenticatedPrincipal staff = AuthenticatedPrincipal.staff(UUID.randomUUID(), TENANT, "SUPER_ADMIN");
        AuthenticatedPrincipal player = AuthenticatedPrincipal.global(UUID.randomUUID(), AccountType.PLAYER, "PLAYER");

        for (AuthenticatedPrincipal principal : new AuthenticatedPrincipal[] {staff, player}) {
            assertThatThrownBy(() -> as(principal, guard::requirePlatformUser))
                    .isInstanceOf(PlatformCallerForbiddenException.class);
            assertThatThrownBy(() -> as(principal, () -> guard.require(PAYMENTS)))
                    .isInstanceOf(PlatformCallerForbiddenException.class);
        }
    }

    @Test
    void aRoleThatNoLongerExistsIsForbidden() {
        AuthenticatedPrincipal legacyOwner = platform("PLATFORM_OWNER");

        assertThatThrownBy(() -> as(legacyOwner, guard::requirePlatformUser))
                .isInstanceOf(PlatformCallerForbiddenException.class);
    }

    @Test
    void theSuperAdminHasEveryCapabilityWithoutEnablingAny() {
        AuthenticatedPrincipal superAdmin = platform(PlatformRole.SUPER_ADMIN.name());

        for (Capability capability : Capability.values()) {
            assertThat(as(superAdmin, () -> guard.require(capability)).role()).isEqualTo(PlatformRole.SUPER_ADMIN);
        }
        PlatformCaller caller = as(superAdmin, guard::requireSuperAdmin);
        assertThat(guard.effectiveCapabilities(caller)).isEqualTo(EnumSet.allOf(Capability.class));
    }

    @Test
    void anOperatorWithoutTheCapabilityGetsCapabilityRevoked() {
        AuthenticatedPrincipal operator = platform(PlatformRole.OPERATOR.name());

        assertThatThrownBy(() -> as(operator, () -> guard.require(PAYMENTS)))
                .isInstanceOf(CapabilityRevokedException.class)
                .extracting(failure -> ((CapabilityRevokedException) failure).capability())
                .isEqualTo(PAYMENTS);
    }

    @Test
    void anOperatorNeverReachesOperationsReservedToTheSuperAdmin() {
        capabilities.set(PAYMENTS, true);
        AuthenticatedPrincipal operator = platform(PlatformRole.OPERATOR.name());

        assertThatThrownBy(() -> as(operator, guard::requireSuperAdmin))
                .isInstanceOf(PlatformCallerForbiddenException.class);
    }

    @Test
    void aChangeOfTheEnabledCapabilitiesAppliesOnTheNextCall() {
        AuthenticatedPrincipal operator = platform(PlatformRole.OPERATOR.name());

        capabilities.set(PAYMENTS, true);
        PlatformCaller caller = as(operator, () -> guard.require(PAYMENTS));
        assertThat(guard.effectiveCapabilities(caller)).containsExactly(PAYMENTS);

        capabilities.set(PAYMENTS, false);
        assertThatThrownBy(() -> as(operator, () -> guard.require(PAYMENTS)))
                .isInstanceOf(CapabilityRevokedException.class);
        assertThat(guard.effectiveCapabilities(caller)).isEmpty();
    }

    private static AuthenticatedPrincipal platform(String role) {
        return AuthenticatedPrincipal.global(UUID.randomUUID(), AccountType.PLATFORM, role);
    }

    private static <T> T as(AuthenticatedPrincipal principal, Supplier<T> operation) {
        return CurrentPrincipal.call(principal, operation::get);
    }
}
