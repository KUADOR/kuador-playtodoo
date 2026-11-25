package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;
import com.kuador.platodo.api.bookings.domain.Booking;

/** A player reads one of their bookings with its effective status (an overdue pending one is EXPIRED). */
public class GetBooking {

    private final BookingRepository bookings;
    private final CurrentPlayer currentPlayer;
    private final Clock clock;

    public GetBooking(BookingRepository bookings, CurrentPlayer currentPlayer, Clock clock) {
        this.bookings = bookings;
        this.currentPlayer = currentPlayer;
        this.clock = clock;
    }

    /**
     * @throws BookingNotFoundException if the booking does not exist or belongs to another player
     */
    public Booking get(UUID bookingId) {
        return OwnBookings.require(bookings, bookingId, currentPlayer.require()).asSeenAt(clock.instant());
    }
}
