package com.kuador.platodo.api.bookings.support;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.NotStaffOfTenantException;
import com.kuador.platodo.api.bookings.application.StaffNotAuthenticatedException;
import com.kuador.platodo.api.bookings.application.port.out.CurrentStaff;

/**
 * The staff member the current thread acts as. By default nobody is authenticated; a caller can also be
 * authenticated but not staff of the active tenant.
 */
public final class StubCurrentStaff implements CurrentStaff {

    private final ThreadLocal<UUID> staff = new ThreadLocal<>();
    private final ThreadLocal<Boolean> outsider = ThreadLocal.withInitial(() -> false);

    public void actAs(UUID staffId) {
        staff.set(staffId);
        outsider.set(false);
    }

    /** Authenticated, but without a staff role in the active tenant. */
    public void actAsOutsider() {
        staff.set(UUID.randomUUID());
        outsider.set(true);
    }

    @Override
    public UUID require() {
        UUID staffId = staff.get();
        if (staffId == null) {
            throw new StaffNotAuthenticatedException();
        }
        if (outsider.get()) {
            throw new NotStaffOfTenantException();
        }
        return staffId;
    }
}
