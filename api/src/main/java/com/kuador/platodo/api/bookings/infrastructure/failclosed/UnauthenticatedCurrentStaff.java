package com.kuador.platodo.api.bookings.infrastructure.failclosed;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.StaffNotAuthenticatedException;
import com.kuador.platodo.api.bookings.application.port.out.CurrentStaff;

/** Used until Identity and Access authenticates staff: every request is rejected as unauthenticated. */
public class UnauthenticatedCurrentStaff implements CurrentStaff {

    @Override
    public UUID require() {
        throw new StaffNotAuthenticatedException();
    }
}
