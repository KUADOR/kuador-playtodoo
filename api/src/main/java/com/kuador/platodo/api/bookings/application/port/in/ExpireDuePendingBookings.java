package com.kuador.platodo.api.bookings.application.port.in;

/** Releases the slots of pending bookings whose hold has ended in the tenant whose schema is active. */
@FunctionalInterface
public interface ExpireDuePendingBookings {

    /**
     * Marks due pending bookings as expired and announces each one.
     *
     * @param tenantId tenant whose schema is active on the current thread; it is only copied into the events
     * @return how many bookings expired
     */
    int expireDue(String tenantId);
}
