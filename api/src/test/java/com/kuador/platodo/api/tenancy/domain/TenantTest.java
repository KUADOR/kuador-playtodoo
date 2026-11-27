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

/** Lifecycle of ADR-013 as adjusted by ADR-014. */
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

    /** ADR-014: a failed provisioning waits in PROVISIONING_FAILED and a retry goes back to PROVISIONING. */
    @Test
    void aFailedProvisioningIsRetriedUntilTheComplexIsActive() {
        Tenant failed = provisioning().failProvisioning();
        Tenant retrying = failed.retryProvisioning();

        assertThat(failed.status()).isEqualTo(TenantStatus.PROVISIONING_FAILED);
        assertThat(failed.createdAt()).isEqualTo(T0);
        assertThat(retrying.status()).isEqualTo(TenantStatus.PROVISIONING);
        assertThat(retrying.activate(T1).status()).isEqualTo(TenantStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "PROVISIONING", mode = EnumSource.Mode.EXCLUDE)
    void onlyAProvisioningComplexCanFailItsProvisioning(TenantStatus status) {
        assertInvalid(status, Tenant::failProvisioning);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "PROVISIONING_FAILED", mode = EnumSource.Mode.EXCLUDE)
    void onlyAFailedProvisioningCanBeRetried(TenantStatus status) {
        assertInvalid(status, Tenant::retryProvisioning);
    }

    /** ADR-014: suspending starts the retention from the suspension. */
    @Test
    void suspendingSetsTheRetentionFromTheSuspension() {
        Tenant suspended = active().suspend(T1, RETENTION);

        assertThat(suspended.status()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(suspended.suspendedAt()).isEqualTo(T1);
        assertThat(suspended.retentionDays()).isEqualTo(RETENTION);
        assertThat(suspended.purgeDueAt()).isEqualTo(T1.plus(Duration.ofDays(RETENTION)));
        assertThat(suspended.archivedAt()).isNull();
    }

    /** ADR-014: a suspended complex reactivated before its purge date is active again, without retention. */
    @Test
    void reactivatingASuspendedComplexBeforeThePurgeDateClearsTheRetention() {
        Tenant suspended = active().suspend(T1, RETENTION);

        Tenant reactivated = suspended.reactivate(suspended.purgeDueAt().minusMillis(1));

        assertThat(reactivated.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(reactivated.suspendedAt()).isNull();
        assertThat(reactivated.retentionDays()).isNull();
        assertThat(reactivated.purgeDueAt()).isNull();
    }

    @Test
    void reactivatingFromThePurgeDateOnIsRefused() {
        Tenant suspended = active().suspend(T1, RETENTION);

        assertThatThrownBy(() -> suspended.reactivate(suspended.purgeDueAt()))
                .isInstanceOf(RetentionExpiredException.class);
    }

    /** ADR-014: cancelling an active complex archives it with the retention from the cancellation. */
    @Test
    void archivingAnActiveComplexSetsTheRetentionFromTheArchive() {
        Tenant archived = active().archive(T1, RETENTION);

        assertThat(archived.status()).isEqualTo(TenantStatus.ARCHIVED);
        assertThat(archived.archivedAt()).isEqualTo(T1);
        assertThat(archived.suspendedAt()).isNull();
        assertThat(archived.retentionDays()).isEqualTo(RETENTION);
        assertThat(archived.purgeDueAt()).isEqualTo(T1.plus(Duration.ofDays(RETENTION)));
    }

    /** ADR-014: cancelling a suspended complex keeps the purge date that was running. */
    @Test
    void archivingASuspendedComplexKeepsItsRetention() {
        Tenant suspended = active().suspend(T1, 30);
        Instant tenDaysLater = T1.plus(Duration.ofDays(10));

        Tenant archived = suspended.archive(tenDaysLater, RETENTION);

        assertThat(archived.status()).isEqualTo(TenantStatus.ARCHIVED);
        assertThat(archived.archivedAt()).isEqualTo(tenDaysLater);
        assertThat(archived.suspendedAt()).isEqualTo(T1);
        assertThat(archived.retentionDays()).isEqualTo(30);
        assertThat(archived.purgeDueAt()).isEqualTo(suspended.purgeDueAt());
    }

    /** ADR-014: a cancelled subscription is terminal, so an archived complex is never reactivated. */
    @Test
    void anArchivedComplexIsNeverReactivated() {
        Tenant archived = active().archive(T1, RETENTION);

        assertThatThrownBy(() -> archived.reactivate(T1)).isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void theRetentionOfASuspendedComplexIsRecomputedFromTheSuspension() {
        Tenant suspended = active().suspend(T1, RETENTION);

        Tenant extended = suspended.changeRetention(60, T1.plus(Duration.ofDays(10)));

        assertThat(extended.status()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(extended.retentionDays()).isEqualTo(60);
        assertThat(extended.purgeDueAt()).isEqualTo(T1.plus(Duration.ofDays(60)));
    }

    @Test
    void theRetentionOfAComplexCancelledWhileActiveIsRecomputedFromTheArchive() {
        Tenant archived = active().archive(T1, RETENTION);

        Tenant extended = archived.changeRetention(60, T1.plus(Duration.ofDays(10)));

        assertThat(extended.status()).isEqualTo(TenantStatus.ARCHIVED);
        assertThat(extended.retentionDays()).isEqualTo(60);
        assertThat(extended.purgeDueAt()).isEqualTo(T1.plus(Duration.ofDays(60)));
        assertThat(extended.archivedAt()).isEqualTo(T1);
    }

    @Test
    void theRetentionOfAComplexCancelledWhileSuspendedIsRecomputedFromTheSuspension() {
        Tenant archived = active().suspend(T1, RETENTION).archive(T1.plus(Duration.ofDays(5)), RETENTION);

        Tenant extended = archived.changeRetention(60, T1.plus(Duration.ofDays(10)));

        assertThat(extended.purgeDueAt()).isEqualTo(T1.plus(Duration.ofDays(60)));
        assertThat(extended.archivedAt()).isEqualTo(T1.plus(Duration.ofDays(5)));
    }

    @Test
    void aRetentionThatPutsThePurgeDateInThePastIsRefused() {
        Tenant suspended = active().suspend(T1, RETENTION);
        Instant tenDaysLater = T1.plus(Duration.ofDays(10));

        assertThatThrownBy(() -> suspended.changeRetention(10, tenDaysLater))
                .isInstanceOf(RetentionInvalidException.class);
        assertThatThrownBy(() -> suspended.changeRetention(5, tenDaysLater))
                .isInstanceOf(RetentionInvalidException.class);
    }

    @Test
    void aRetentionBelowOneDayIsRefused() {
        assertThatIllegalArgumentException().isThrownBy(() -> active().archive(T1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> active().suspend(T1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> active().archive(T1, RETENTION).changeRetention(0, T1));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "PROVISIONING", mode = EnumSource.Mode.EXCLUDE)
    void onlyProvisioningComplexesCanBeActivated(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.activate(T1));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void onlyActiveComplexesCanBeSuspended(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.suspend(T1, RETENTION));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class,
            names = {"PROVISIONING", "PROVISIONING_FAILED", "ARCHIVED", "PURGING", "PURGED"})
    void onlyActiveAndSuspendedComplexesCanBeArchived(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.archive(T1, RETENTION));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "SUSPENDED", mode = EnumSource.Mode.EXCLUDE)
    void onlySuspendedComplexesCanBeReactivated(TenantStatus status) {
        assertInvalid(status, tenant -> tenant.reactivate(T1));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"SUSPENDED", "ARCHIVED"}, mode = EnumSource.Mode.EXCLUDE)
    void onlySuspendedAndArchivedComplexesChangeTheirRetention(TenantStatus status) {
        Tenant tenant = new Tenant(ID, "Canchas El Sol", status, T0, null, null, null, null, null, null);

        assertThatThrownBy(() -> tenant.changeRetention(RETENTION, T1)).isInstanceOf(NotInRetentionException.class);
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
