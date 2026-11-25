package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CurrentStaff;
import com.kuador.platodo.api.bookings.domain.Booking;

/** The complex staff reads any booking or block of the complex with its effective status. */
public class GetStaffBooking {

    private final BookingRepository bookings;
    private final CurrentStaff currentStaff;
    private final Clock clock;

    public GetStaffBooking(BookingRepository bookings, CurrentStaff currentStaff, Clock clock) {
        this.bookings = bookings;
        this.currentStaff = currentStaff;
        this.clock = clock;
    }

    /**
     * @throws BookingNotFoundException if the booking does not exist in the active tenant
     */
    public Booking get(UUID bookingId) {
        currentStaff.require();
        return bookings.findById(bookingId)
                .map(booking -> booking.asSeenAt(clock.instant()))
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }
}
