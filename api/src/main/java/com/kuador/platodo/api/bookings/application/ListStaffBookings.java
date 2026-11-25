package com.kuador.platodo.api.bookings.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CurrentStaff;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.TimePeriod;

/**
 * The staff calendar: bookings and blocks that occupy their slot (confirmed, or pending and not overdue)
 * in a range, ordered by start.
 */
public class ListStaffBookings {

    private final BookingRepository bookings;
    private final CurrentStaff currentStaff;
    private final Clock clock;

    public ListStaffBookings(BookingRepository bookings, CurrentStaff currentStaff, Clock clock) {
        this.bookings = bookings;
        this.currentStaff = currentStaff;
        this.clock = clock;
    }

    /**
     * @param courtId restricts the result to one court when present
     * @throws InvalidOccupancyRangeException if {@code to} is not after {@code from} or the range is too long
     */
    public List<Booking> list(Instant from, Instant to, Optional<UUID> courtId) {
        currentStaff.require();
        TimePeriod range = CalendarRange.of(from, to);
        Instant now = clock.instant();
        return courtId
                .map(court -> bookings.findActiveOfCourtOverlapping(court, range, now))
                .orElseGet(() -> bookings.findActiveOverlapping(range, now));
    }
}
