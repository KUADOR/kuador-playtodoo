package com.kuador.platodo.api.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.identity.application.port.out.CapabilityChangeLog.RecordedChange;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.identity.domain.CapabilityToggle;
import com.kuador.platodo.api.identity.domain.OperatorCapabilitySettings;
import com.kuador.platodo.api.identity.domain.PlatformRole;
import com.kuador.platodo.api.identity.domain.event.OperatorCapabilitiesChanged;
import com.kuador.platodo.api.identity.support.InMemoryOperatorCapabilities;

/** Batch changes of operator capabilities with optimistic concurrency and a change record (PLATODO-82, CA16). */
class ChangeOperatorCapabilitiesTest {

    private static final Instant NOW = Instant.parse("2027-01-05T08:00:00Z");
    private static final CapabilityToggle ENABLE_PAYMENTS =
            new CapabilityToggle(Capability.SUBSCRIPTION_PAYMENT_RECORD, true);

    private final InMemoryOperatorCapabilities capabilities = new InMemoryOperatorCapabilities();
    private final List<OperatorCapabilitiesChanged> published = new ArrayList<>();
    private final ChangeOperatorCapabilities change = new ChangeOperatorCapabilities(
            new CapabilityGuard(capabilities), capabilities, capabilities, published::add,
            TransactionOperations.withoutTransaction(), Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID superAdminId = UUID.randomUUID();

    @Test
    void anEffectiveChangeBumpsTheVersionIsRecordedAndPublished() {
        OperatorCapabilitySettings after = asSuperAdmin(() -> change.change(0, List.of(ENABLE_PAYMENTS)));

        assertThat(after.version()).isEqualTo(1);
        assertThat(after.enabledCapabilities()).containsExactly(Capability.SUBSCRIPTION_PAYMENT_RECORD);
        assertThat(capabilities.current()).isEqualTo(after);
        assertThat(capabilities.recorded()).singleElement().satisfies(recorded -> {
            assertThat(recorded.capability()).isEqualTo(Capability.SUBSCRIPTION_PAYMENT_RECORD);
            assertThat(recorded.enabled()).isTrue();
            assertThat(recorded.changedBy()).isEqualTo(superAdminId);
            assertThat(recorded.changedAt()).isEqualTo(NOW);
        });
        assertThat(published).containsExactly(
                new OperatorCapabilitiesChanged(List.of(ENABLE_PAYMENTS), superAdminId, NOW));
    }

    @Test
    void onlyTheCapabilitiesThatChangeAreRecorded() {
        CapabilityToggle keepStatusDisabled = new CapabilityToggle(Capability.SUBSCRIPTION_STATUS_CHANGE, false);

        asSuperAdmin(() -> change.change(0, List.of(ENABLE_PAYMENTS, keepStatusDisabled)));

        assertThat(capabilities.recorded()).extracting(RecordedChange::capability)
                .containsExactly(Capability.SUBSCRIPTION_PAYMENT_RECORD);
    }

    @Test
    void aBatchThatChangesNothingKeepsTheVersionAndRecordsNothing() {
        CapabilityToggle alreadyDisabled = new CapabilityToggle(Capability.COMPLEX_CREATE, false);

        OperatorCapabilitySettings after = asSuperAdmin(() -> change.change(0, List.of(alreadyDisabled)));

        assertThat(after.version()).isZero();
        assertThat(capabilities.recorded()).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void aStaleVersionChangesNothing() {
        capabilities.set(Capability.COMPLEX_CREATE, true);

        assertThatThrownBy(() -> asSuperAdmin(() -> change.change(0, List.of(ENABLE_PAYMENTS))))
                .isInstanceOf(StaleOperatorCapabilitiesException.class);
        assertThat(capabilities.current().isEnabled(Capability.SUBSCRIPTION_PAYMENT_RECORD)).isFalse();
        assertThat(capabilities.recorded()).isEmpty();
    }

    @Test
    void anEmptyBatchOrARepeatedCapabilityIsInvalid() {
        assertThatThrownBy(() -> asSuperAdmin(() -> change.change(0, List.of())))
                .isInstanceOf(InvalidCapabilityChangeException.class);
        assertThatThrownBy(() -> asSuperAdmin(() -> change.change(0, List.of(
                        ENABLE_PAYMENTS, new CapabilityToggle(Capability.SUBSCRIPTION_PAYMENT_RECORD, false)))))
                .isInstanceOf(InvalidCapabilityChangeException.class);
    }

    @Test
    void anOperatorCannotChangeCapabilities() {
        AuthenticatedPrincipal operator =
                AuthenticatedPrincipal.global(UUID.randomUUID(), AccountType.PLATFORM, PlatformRole.OPERATOR.name());

        assertThatThrownBy(() -> CurrentPrincipal.call(operator, () -> change.change(0, List.of(ENABLE_PAYMENTS))))
                .isInstanceOf(PlatformCallerForbiddenException.class);
        assertThat(capabilities.current().version()).isZero();
    }

    private <T> T asSuperAdmin(Supplier<T> operation) {
        AuthenticatedPrincipal superAdmin =
                AuthenticatedPrincipal.global(superAdminId, AccountType.PLATFORM, PlatformRole.SUPER_ADMIN.name());
        return CurrentPrincipal.call(superAdmin, operation::get);
    }
}
