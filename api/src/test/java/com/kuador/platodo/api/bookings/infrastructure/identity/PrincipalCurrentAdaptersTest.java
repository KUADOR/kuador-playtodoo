package com.kuador.platodo.api.bookings.infrastructure.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.bookings.application.NotStaffOfTenantException;
import com.kuador.platodo.api.bookings.application.PlayerNotAuthenticatedException;
import com.kuador.platodo.api.bookings.application.StaffNotAuthenticatedException;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

class PrincipalCurrentAdaptersTest {

    private static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final TenantId B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");

    private final PrincipalCurrentPlayer currentPlayer = new PrincipalCurrentPlayer();
    private final Set<UUID> supportGranted = new HashSet<>();
    private final PrincipalCurrentStaff currentStaff = new PrincipalCurrentStaff(supportGranted::contains);

    @Test
    void thePlayerIsTheGlobalAccountOfTheToken() {
        UUID playerId = UUID.randomUUID();

        UUID current = CurrentPrincipal.call(AuthenticatedPrincipal.global(playerId, AccountType.PLAYER, "PLAYER"),
                currentPlayer::require);

        assertThat(current).isEqualTo(playerId);
    }

    @Test
    void withoutAPlayerTokenThereIsNoPlayer() {
        assertThatThrownBy(currentPlayer::require).isInstanceOf(PlayerNotAuthenticatedException.class);
        assertThatThrownBy(() -> TenantScope.call(A, () -> CurrentPrincipal.call(
                        AuthenticatedPrincipal.staff(UUID.randomUUID(), A, "STAFF"), currentPlayer::require)))
                .isInstanceOf(PlayerNotAuthenticatedException.class);
    }

    @Test
    void theStaffMemberMustBelongToTheTenantInScope() {
        UUID staffId = UUID.randomUUID();
        AuthenticatedPrincipal staffOfA = AuthenticatedPrincipal.staff(staffId, A, "STAFF");

        assertThat(TenantScope.call(A, () -> CurrentPrincipal.call(staffOfA, currentStaff::require)))
                .isEqualTo(staffId);
        assertThatThrownBy(() -> TenantScope.call(B, () -> CurrentPrincipal.call(staffOfA, currentStaff::require)))
                .isInstanceOf(NotStaffOfTenantException.class);
    }

    /** ADR-020: Kuador support acts as staff only on a request the guard granted to that platform user. */
    @Test
    void aPlatformUserActsAsStaffOnlyWithASupportGrant() {
        UUID supportId = UUID.randomUUID();
        AuthenticatedPrincipal support = AuthenticatedPrincipal.global(supportId, AccountType.PLATFORM, "OPERATOR");

        assertThatThrownBy(() -> TenantScope.call(A, () -> CurrentPrincipal.call(support, currentStaff::require)))
                .isInstanceOf(NotStaffOfTenantException.class);

        supportGranted.add(supportId);
        assertThat(TenantScope.call(A, () -> CurrentPrincipal.call(support, currentStaff::require)))
                .isEqualTo(supportId);
    }

    @Test
    void aSupportGrantNeverMakesAPlayerStaff() {
        UUID playerId = UUID.randomUUID();
        supportGranted.add(playerId);
        AuthenticatedPrincipal player = AuthenticatedPrincipal.global(playerId, AccountType.PLAYER, "PLAYER");

        assertThatThrownBy(() -> TenantScope.call(A, () -> CurrentPrincipal.call(player, currentStaff::require)))
                .isInstanceOf(NotStaffOfTenantException.class);
    }

    @Test
    void aPlayerIsNotStaffAndNoTokenIsUnauthenticated() {
        AuthenticatedPrincipal player = AuthenticatedPrincipal.global(UUID.randomUUID(), AccountType.PLAYER, "PLAYER");

        assertThatThrownBy(() -> TenantScope.call(A, () -> CurrentPrincipal.call(player, currentStaff::require)))
                .isInstanceOf(NotStaffOfTenantException.class);
        assertThatThrownBy(() -> TenantScope.call(A, currentStaff::require))
                .isInstanceOf(StaffNotAuthenticatedException.class);
    }
}
