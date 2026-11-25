package com.kuador.platodo.api.bookings.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.tenancy.application.port.in.ActiveTenantRunner;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

class ExpirePendingBookingsJobTest {

    private static final List<TenantId> TENANTS = List.of(new TenantId("tenant-a"), new TenantId("tenant-b"));

    private final ActiveTenantRunner activeTenants = work -> TENANTS.forEach(work);

    @Test
    void expiresPendingBookingsInEveryActiveTenant() {
        List<String> visited = new ArrayList<>();
        ExpirePendingBookingsJob job = new ExpirePendingBookingsJob(activeTenants, tenantId -> {
            visited.add(tenantId);
            return 0;
        });

        job.run();

        assertThat(visited).containsExactly("tenant-a", "tenant-b");
    }

    @Test
    void aFailingTenantDoesNotStopTheOthers() {
        List<String> visited = new ArrayList<>();
        ExpirePendingBookingsJob job = new ExpirePendingBookingsJob(activeTenants, tenantId -> {
            if (tenantId.equals("tenant-a")) {
                throw new IllegalStateException("schema unavailable");
            }
            visited.add(tenantId);
            return 1;
        });

        assertThatNoException().isThrownBy(job::run);
        assertThat(visited).containsExactly("tenant-b");
    }
}
