package com.kuador.platodo.api.bookings.domain;

import java.util.Objects;

/**
 * Person a staff manual booking is held for. Personal data: it stays in the tenant schema
 * and never goes to logs or events.
 */
public record BookingHolder(String name, String phone) {

    public BookingHolder {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(phone, "phone");
    }

    @Override
    public String toString() {
        return "BookingHolder[redacted]";
    }
}
