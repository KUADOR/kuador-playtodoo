package com.kuador.platodo.api.bookings.infrastructure.web;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.bookings.application.BlockSlot;
import com.kuador.platodo.api.bookings.application.CancelBookingByStaff;
import com.kuador.platodo.api.bookings.application.CreateStaffBooking;
import com.kuador.platodo.api.bookings.application.GetStaffBooking;
import com.kuador.platodo.api.bookings.application.ListStaffBookings;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;

/** Staff booking endpoints of {@code contracts/bookings.openapi.yaml}; every entry carries {@code inactive} (ADR-019). */
@RestController
@RequestMapping(StaffBookingController.STAFF_PATH)
@ConditionalOnStaffApiEnabled
class StaffBookingController {

    static final String STAFF_PATH = ApiPaths.V1 + "/staff";
    private static final String BOOKINGS = "/bookings";

    private final ListStaffBookings listStaffBookings;
    private final GetStaffBooking getStaffBooking;
    private final CreateStaffBooking createStaffBooking;
    private final BlockSlot blockSlot;
    private final CancelBookingByStaff cancelBookingByStaff;
    private final BookingInactivity inactivity;

    StaffBookingController(ListStaffBookings listStaffBookings, GetStaffBooking getStaffBooking,
            CreateStaffBooking createStaffBooking, BlockSlot blockSlot, CancelBookingByStaff cancelBookingByStaff,
            TenantDirectory tenantDirectory) {
        this.listStaffBookings = listStaffBookings;
        this.getStaffBooking = getStaffBooking;
        this.createStaffBooking = createStaffBooking;
        this.blockSlot = blockSlot;
        this.cancelBookingByStaff = cancelBookingByStaff;
        this.inactivity = new BookingInactivity(tenantDirectory);
    }

    @GetMapping(BOOKINGS)
    List<StaffBookingResponse> list(
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam Optional<UUID> courtId) {
        List<Booking> bookings = listStaffBookings.list(from.toInstant(), to.toInstant(), courtId);
        if (bookings.isEmpty()) {
            return List.of();
        }
        boolean inactive = inactivity.ofResolvedTenant();
        return bookings.stream().map(booking -> StaffBookingResponse.of(booking, inactive)).toList();
    }

    @GetMapping(BOOKINGS + "/{bookingId}")
    StaffBookingResponse get(@PathVariable UUID bookingId) {
        return answer(getStaffBooking.get(bookingId));
    }

    @PostMapping(BOOKINGS)
    ResponseEntity<StaffBookingResponse> create(@RequestBody CreateStaffBookingRequest request) {
        return created(createStaffBooking.create(request.toCommand()));
    }

    @PostMapping("/blocks")
    ResponseEntity<StaffBookingResponse> block(@RequestBody BlockSlotRequest request) {
        return created(blockSlot.block(request.toCommand()));
    }

    @PostMapping(BOOKINGS + "/{bookingId}/cancel")
    StaffBookingResponse cancel(@PathVariable UUID bookingId) {
        return answer(cancelBookingByStaff.cancel(bookingId));
    }

    private ResponseEntity<StaffBookingResponse> created(Booking booking) {
        return ResponseEntity.created(URI.create(STAFF_PATH + BOOKINGS + "/" + booking.id()))
                .body(answer(booking));
    }

    private StaffBookingResponse answer(Booking booking) {
        return StaffBookingResponse.of(booking, inactivity.ofResolvedTenant());
    }
}
