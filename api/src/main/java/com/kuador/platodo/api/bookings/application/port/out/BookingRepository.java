package com.kuador.platodo.api.bookings.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;

/**
 * Bookings of the tenant whose schema is active on the current connection.
 * Implementations never read or write another tenant's schema.
 */
public interface BookingRepository {

    /**
     * Stores a new booking.
     *
     * @throws BookingOverlapException if it is active and overlaps another active booking of the same court
     */
    void add(Booking booking);

    Optional<Booking> findById(UUID id);
}
