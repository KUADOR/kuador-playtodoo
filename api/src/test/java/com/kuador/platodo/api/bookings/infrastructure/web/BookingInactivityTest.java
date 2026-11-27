package com.kuador.platodo.api.bookings.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kuador.platodo.api.tenancy.application.port.in.NoTenantInScopeException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** ADR-019: «inactive» is derived from the status of the resolved tenant, never stored. */
class BookingInactivityTest {

    private static final TenantId TENANT = new TenantId("5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c");

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"SUSPENDED", "ARCHIVED"})
    void entriesOfASuspendedOrArchivedComplexAreInactive(TenantStatus status) {
        assertThat(inScope(status)).isTrue();
    }

    @Test
    void entriesOfAnActiveComplexAreActive() {
        assertThat(inScope(TenantStatus.ACTIVE)).isFalse();
    }

    @Test
    void withoutATenantInScopeItFailsClosed() {
        BookingInactivity inactivity = new BookingInactivity(directoryWith(TenantStatus.ACTIVE));

        assertThatThrownBy(inactivity::ofResolvedTenant).isInstanceOf(NoTenantInScopeException.class);
    }

    private static boolean inScope(TenantStatus status) {
        BookingInactivity inactivity = new BookingInactivity(directoryWith(status));
        return TenantScope.call(TENANT, inactivity::ofResolvedTenant);
    }

    private static TenantDirectory directoryWith(TenantStatus status) {
        return new TenantDirectory() {
            @Override
            public Optional<TenantEntry> find(TenantId tenant) {
                return Optional.of(new TenantEntry(tenant, status, Optional.empty()));
            }

            @Override
            public List<TenantProfile> profiles(Collection<TenantId> tenants) {
                return List.of();
            }
        };
    }
}
