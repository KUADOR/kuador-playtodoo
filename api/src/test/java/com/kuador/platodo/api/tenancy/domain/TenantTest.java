package com.kuador.platodo.api.tenancy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Lifecycle of ADR-013. Invalid transitions are the 409 INVALID_TRANSITION of the contract. */
class TenantTest {

    private static final TenantId ID = new TenantId("5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c");
    private static final Instant T0 = Instant.parse("2026-12-01T14:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final int RETENTION = 45;

    @Test
    void aNewComplexIsProvisioning() {
        Tenant tenant = Tenant.provisioning(ID, "Canchas El Sol", T0);

        assertThat(tenant.status()).isEqualTo(TenantStatus.PROVISIONING);
        assertThat(tenant.createdAt()).isEqualTo(T0);
        assertThat(tenant.activatedAt()).isNull();
    }

    @Test
    void provisioningBecomesActive() {
        Tenant active = provisioning().activate(T1);

        assertThat(active.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(active.activatedAt()).isEqualTo(T1);
    }

    @Test
    void anActiveComplexIsSuspendedAndActivatedAgain() {
        Tenant suspended = active().suspend(T1);
        Tenant activeAgain = suspended.activate(T1.plusSeconds(1));

        assertThat(suspended.status()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(suspended.suspendedAt()).isEqualTo(T1);
        assertThat(activeAgain.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(activeAgain.suspendedAt()).isNull();
    }

    @Test
    void archivingSetsTheRetentionAndThePurgeDate() {
        Tenant archived = active().archive(T1, RETENTION);

        assertThat(archived.status()).isEqualTo(TenantStatus.ARCHIVED);
        assertThat(archived.archivedAt()).isEqualTo(T1);
        assertThat(archived.retentionDays()).isEqualTo(RETENTION);
        assertThat(archived.purgeDueAt()).isEqualTo(T1.plus(Duration.ofDays(RETENTION)));
    }

    @Test
    void aSuspendedComplexCanBeArchived() {
        assertThat(active().suspend(T1).archive(T1, RETENTION).status()).isEqualTo(TenantStatus.ARCHIVED);
    }

    @Test
    void reactivatingBeforeThePurgeDateClearsTheArchive() {
        Tenant archived = active().suspend(T1).archive(T1, RETENTION);

        Tenant reactivated = archived.reactivate(archived.purgeDueAt().minusMillis(1));

        assertThat(reactivated.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(reactivated.archivedAt()).isNull();
        assertThat(reactivated.retentionDays()).isNull();
        assertThat(reactivated.purgeDueAt()).isNull();
        assertThat(reactivated.suspendedAt()).isNull();
    }

    @Test
    void reactivatingFromThePurgeDateOnIsRefused() {
        Tenant archived = active().archive(T1, RETENTION);

        assertThatThrownBy(() -> archived.reactivate(archived.purgeDueAt()))
                .isInstanceOf(RetentionExpiredException.class);
    }

    @Test
    void theRetentionIsRecomputedFromTheArchiveDate() {
        Tenant archived = active().archive(T1, RETENTION);

        Tenant extended = archived.changeRetention(60, T1.plus(Duration.ofDays(10)));

        assertThat(extended.retentionDays()).isEqualTo(60);
        assertThat(extended.purgeDueAt()).isEqualTo(T1.plus(Duration.ofDays(60)));
        assertThat(extended.archivedAt()).isEqualTo(T1);
    }

    @Test
    void aRetentionThatPutsThePurgeDateInThePastIsRefused() {
        Tenant archived = active().archive(T1, RETENTION);
        Instant tenDaysLater = T1.plus(Duration.ofDays(10));

        assertThatThrownBy(() -> archived.changeRetention(10, tenDaysLater))
                .isInstanceOf(RetentionInvalidException.class);
        assertThatThrownBy(() -> archived.changeRetention(5, tenDaysLater))
                .isInstanceOf(RetentionInvalidException.class);
    }

    @Test
    void aRetentionBelowOneDayIsRefused() {
        assertThatIllegalArgumentException().isThrownBy(() -> active().archive(T1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> active().archive(T1, RETENTION).changeRetention(0, T1));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"ACTIVE", "ARCHIVED", "PURGING", "PURGED"})
    void onlyProvisioningAndSuspendedComplexesCanBeActivated(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.activate(T1));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void onlyActiveComplexesCanBeSuspended(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.suspend(T1));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"PROVISIONING", "ARCHIVED", "PURGING", "PURGED"})
    void onlyActiveAndSuspendedComplexesCanBeArchived(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.archive(T1, RETENTION));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "ARCHIVED", mode = EnumSource.Mode.EXCLUDE)
    void onlyArchivedComplexesCanBeReactivatedOrChangeTheirRetention(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.reactivate(T1));
        assertInvalid(status, tenant -> tenant.changeRetention(RETENTION, T1));
    }

    /** Criterion 13: the purge starts once the purge date is reached and keeps the archive fields. */
    @Test
    void anArchivedComplexStartsItsPurgeFromThePurgeDateOn() {
        Tenant archived = active().archive(T1, RETENTION);

        Tenant purging = archived.startPurge(archived.purgeDueAt());

        assertThat(purging.status()).isEqualTo(TenantStatus.PURGING);
        assertThat(purging.archivedAt()).isEqualTo(T1);
        assertThat(purging.purgeDueAt()).isEqualTo(archived.purgeDueAt());
        assertThat(purging.purgedAt()).isNull();
    }

    @Test
    void thePurgeNeverStartsBeforeThePurgeDate() {
        Tenant archived = active().archive(T1, RETENTION);

        assertThatThrownBy(() -> archived.startPurge(archived.purgeDueAt().minusMillis(1)))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "ARCHIVED", mode = EnumSource.Mode.EXCLUDE)
    void onlyArchivedComplexesStartAPurge(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.startPurge(T1));
    }

    /** Criterion 16: the tombstone keeps the id, the commercial name, the state and the dates. */
    @Test
    void aPurgedComplexKeepsItsTombstone() {
        Tenant archived = active().archive(T1, RETENTION);
        Instant purgedAt = archived.purgeDueAt().plusSeconds(5);

        Tenant purged = archived.startPurge(archived.purgeDueAt()).markPurged(purgedAt);

        assertThat(purged.status()).isEqualTo(TenantStatus.PURGED);
        assertThat(purged.purgedAt()).isEqualTo(purgedAt);
        assertThat(purged.id()).isEqualTo(ID);
        assertThat(purged.displayName()).isEqualTo("Canchas El Sol");
        assertThat(purged.createdAt()).isEqualTo(T0);
        assertThat(purged.archivedAt()).isEqualTo(T1);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "PURGING", mode = EnumSource.Mode.EXCLUDE)
    void onlyPurgingComplexesBecomePurged(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.markPurged(T1));
    }

    @Test
    void theDisplayNameIsRequiredAndBounded() {
        assertThatIllegalArgumentException().isThrownBy(() -> Tenant.provisioning(ID, " ", T0));
        assertThatIllegalArgumentException().isThrownBy(
                () -> Tenant.provisioning(ID, "x".repeat(Tenant.MAX_DISPLAY_NAME_LENGTH + 1), T0));
    }

    private static void assertInvalid(TenantStatus status, UnaryOperator<Tenant> transition) {
        Tenant tenant = new Tenant(ID, "Canchas El Sol", status, T0, null, null, null, null, null, null);

        assertThatThrownBy(() -> transition.apply(tenant)).isInstanceOf(InvalidTransitionException.class);
    }

    private static Tenant provisioning() {
        return Tenant.provisioning(ID, "Canchas El Sol", T0);
    }

    private static Tenant active() {
        return provisioning().activate(T0);
    }
}
