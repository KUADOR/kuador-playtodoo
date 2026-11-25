package com.kuador.platodo.api.bookings.application;

/** The caller is authenticated but has no staff role in the tenant of the request. */
public class NotStaffOfTenantException extends RuntimeException {

    public NotStaffOfTenantException() {
        super("The caller is not staff of this complex");
    }
}
