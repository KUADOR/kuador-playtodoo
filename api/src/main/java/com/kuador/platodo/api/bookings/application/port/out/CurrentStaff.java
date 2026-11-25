package com.kuador.platodo.api.bookings.application.port.out;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.NotStaffOfTenantException;
import com.kuador.platodo.api.bookings.application.StaffNotAuthenticatedException;

/** The authenticated staff member of the current request, provided by Identity and Access. */
public interface CurrentStaff {

    /**
     * @return the id of the staff member, who has a staff role in the tenant of the request
     * @throws StaffNotAuthenticatedException if the request has no authenticated user
     * @throws NotStaffOfTenantException      if the user has no staff role in the tenant of the request
     */
    UUID require();
}
