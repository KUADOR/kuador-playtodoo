package com.kuador.platodo.api.bookings.application.port.out;

import java.time.Instant;
import java.util.List;
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

    /**
     * Marks as EXPIRED, in one atomic step, up to {@code limit} pending bookings whose deadline is at or
     * before {@code now}, which frees their slots. Rows another caller is changing at the same moment are
     * left to that caller, so each booking is returned by exactly one caller.
     *
     * @return the bookings this call expired, already with status EXPIRED
     */
    List<Booking> expirePendingDueBy(Instant now, int limit);
}
