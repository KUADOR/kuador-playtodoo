package com.kuador.platodo.api.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;
import com.kuador.platodo.api.tenancy.domain.event.TenantStatusChanged;

/** ADR-015: a complex entering PURGING loses its open support sessions; no other status change touches them. */
class InvalidateAccessSessionsOfPurgingTenantTest {

    private static final TenantId TENANT = new TenantId("5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c");
    private static final Instant AT = Instant.parse("2026-12-01T03:00:00Z");

    private final List<String> closed = new ArrayList<>();
    private final InvalidateAccessSessionsOfPurgingTenant invalidate =
            new InvalidateAccessSessionsOfPurgingTenant((tenant, at) -> {
                closed.add(tenant.value() + " " + at);
                return 2;
            });

    @Test
    void enteringPurgingClosesTheOpenSessionsOfThatComplexAtTheClaim() {
        int count = invalidate.on(new TenantStatusChanged(TENANT, TenantStatus.PURGING, AT, AT));

        assertThat(count).isEqualTo(2);
        assertThat(closed).containsExactly(TENANT.value() + " " + AT);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "PURGING", mode = EnumSource.Mode.EXCLUDE)
    void otherStatusChangesKeepTheSessions(TenantStatus status) {
        int count = invalidate.on(new TenantStatusChanged(TENANT, status, null, AT));

        assertThat(count).isZero();
        assertThat(closed).isEmpty();
    }
}
