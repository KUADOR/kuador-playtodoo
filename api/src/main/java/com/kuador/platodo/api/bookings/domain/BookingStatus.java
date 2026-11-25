package com.kuador.platodo.api.bookings.domain;

/** Lifecycle of a booking. Only PENDING and CONFIRMED occupy the court slot (ADR-006). */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
