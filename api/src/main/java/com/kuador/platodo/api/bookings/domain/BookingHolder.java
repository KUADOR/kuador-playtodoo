package com.kuador.platodo.api.bookings.domain;

import java.util.Objects;

/**
 * Person a staff manual booking is held for. Personal data: it stays in the tenant schema
 * and never goes to logs or events.
 *
 * @param phone optional contact phone; null when the staff did not register one
 */
public record BookingHolder(String name, String phone) {

    public BookingHolder {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String toString() {
        return "BookingHolder[redacted]";
    }
}
