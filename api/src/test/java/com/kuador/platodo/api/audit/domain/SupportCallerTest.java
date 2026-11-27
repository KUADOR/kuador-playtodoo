package com.kuador.platodo.api.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** Who may work on a complex in each status (ADR-020, PLATODO-27 CA11 and CA15). */
class SupportCallerTest {

    private static final SupportCaller SUPER_ADMIN = new SupportCaller(UUID.randomUUID(), true, true);
    private static final SupportCaller OPERATOR_WITH_ACCESS = new SupportCaller(UUID.randomUUID(), false, true);
    private static final SupportCaller OPERATOR_WITHOUT_ACCESS = new SupportCaller(UUID.randomUUID(), false, false);

    @Test
    void onAnActiveComplexTheSuperAdminAndAnOperatorWithTheCapabilityMayWork() {
        assertThat(SUPER_ADMIN.refusalOn(TenantStatus.ACTIVE)).isEmpty();
        assertThat(OPERATOR_WITH_ACCESS.refusalOn(TenantStatus.ACTIVE)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(TenantStatus.class)
    void anOperatorWithoutTheCapabilityIsRefusedForItWhateverTheStatus(TenantStatus status) {
        assertThat(OPERATOR_WITHOUT_ACCESS.refusalOn(status)).contains(DenialCode.CAPABILITY_REVOKED);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"SUSPENDED", "ARCHIVED"})
    void onASuspendedOrArchivedComplexOnlyTheSuperAdminMayWork(TenantStatus status) {
        assertThat(SUPER_ADMIN.refusalOn(status)).isEmpty();
        assertThat(OPERATOR_WITH_ACCESS.refusalOn(status)).contains(DenialCode.TENANT_NOT_ACCESSIBLE);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"PROVISIONING", "PROVISIONING_FAILED", "PURGING", "PURGED"})
    void nobodyWorksOnAComplexThatIsNotOperating(TenantStatus status) {
        assertThat(SUPER_ADMIN.refusalOn(status)).contains(DenialCode.TENANT_NOT_ACCESSIBLE);
        assertThat(OPERATOR_WITH_ACCESS.refusalOn(status)).contains(DenialCode.TENANT_NOT_ACCESSIBLE);
    }
}
