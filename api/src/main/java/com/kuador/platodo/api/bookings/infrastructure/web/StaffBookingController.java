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

/** Staff booking endpoints of {@code contracts/bookings.openapi.yaml}. */
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

    StaffBookingController(ListStaffBookings listStaffBookings, GetStaffBooking getStaffBooking,
            CreateStaffBooking createStaffBooking, BlockSlot blockSlot, CancelBookingByStaff cancelBookingByStaff) {
        this.listStaffBookings = listStaffBookings;
        this.getStaffBooking = getStaffBooking;
        this.createStaffBooking = createStaffBooking;
        this.blockSlot = blockSlot;
        this.cancelBookingByStaff = cancelBookingByStaff;
    }

    @GetMapping(BOOKINGS)
    List<StaffBookingResponse> list(
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam Optional<UUID> courtId) {
        return listStaffBookings.list(from.toInstant(), to.toInstant(), courtId).stream()
                .map(StaffBookingResponse::of)
                .toList();
    }

    @GetMapping(BOOKINGS + "/{bookingId}")
    StaffBookingResponse get(@PathVariable UUID bookingId) {
        return StaffBookingResponse.of(getStaffBooking.get(bookingId));
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
        return StaffBookingResponse.of(cancelBookingByStaff.cancel(bookingId));
    }

    private static ResponseEntity<StaffBookingResponse> created(Booking booking) {
        return ResponseEntity.created(URI.create(STAFF_PATH + BOOKINGS + "/" + booking.id()))
                .body(StaffBookingResponse.of(booking));
    }
}
