package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.util.List;

import com.kuador.platodo.api.bookings.application.port.in.ExpireDuePendingBookings;
import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingExpired;

/**
 * Scheduled side of the pending-booking expiry (ADR-006, D3). The expiry itself is a single conditional
 * update, so concurrent runs and a racing confirmation never both win, and each expiry is announced once.
 */
public class ExpirePendingBookings implements ExpireDuePendingBookings {

    /**
     * Upper bound of bookings expired per tenant and run, so one busy tenant cannot hold the job.
     * Leftovers expire on the next run; meanwhile lazy expiry and the effective status keep the slot correct.
     */
    public static final int BATCH_SIZE = 500;

    private final BookingRepository bookings;
    private final BookingEventPublisher events;
    private final Clock clock;

    public ExpirePendingBookings(BookingRepository bookings, BookingEventPublisher events, Clock clock) {
        this.bookings = bookings;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public int expireDue(String tenantId) {
        List<Booking> expired = bookings.expirePendingDueBy(clock.instant(), BATCH_SIZE);
        expired.forEach(booking -> events.publish(BookingExpired.of(tenantId, booking)));
        return expired.size();
    }
}
