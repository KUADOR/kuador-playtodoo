package com.kuador.platodo.api.bookings.infrastructure.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.bookings.application.CancelOwnPendingBooking;
import com.kuador.platodo.api.bookings.application.ConfirmBooking;
import com.kuador.platodo.api.bookings.application.CreatePendingBooking;
import com.kuador.platodo.api.bookings.application.GetBooking;
import com.kuador.platodo.api.bookings.domain.Booking;

/** Player booking endpoints of {@code contracts/bookings.openapi.yaml}. */
@RestController
@RequestMapping(PlayerBookingController.BOOKINGS_PATH)
@ConditionalOnPlayerApiEnabled
class PlayerBookingController {

    static final String BOOKINGS_PATH = ApiPaths.V1 + "/bookings";

    private final CreatePendingBooking createPendingBooking;
    private final ConfirmBooking confirmBooking;
    private final CancelOwnPendingBooking cancelOwnPendingBooking;
    private final GetBooking getBooking;

    PlayerBookingController(CreatePendingBooking createPendingBooking, ConfirmBooking confirmBooking,
            CancelOwnPendingBooking cancelOwnPendingBooking, GetBooking getBooking) {
        this.createPendingBooking = createPendingBooking;
        this.confirmBooking = confirmBooking;
        this.cancelOwnPendingBooking = cancelOwnPendingBooking;
        this.getBooking = getBooking;
    }

    @PostMapping
    ResponseEntity<PlayerBookingResponse> create(@RequestBody CreatePendingBookingRequest request) {
        Booking pending = createPendingBooking.create(request.toCommand());
        return ResponseEntity.created(URI.create(BOOKINGS_PATH + "/" + pending.id()))
                .body(PlayerBookingResponse.of(pending));
    }

    @GetMapping("/{bookingId}")
    PlayerBookingResponse get(@PathVariable UUID bookingId) {
        return PlayerBookingResponse.of(getBooking.get(bookingId));
    }

    @PostMapping("/{bookingId}/confirm")
    PlayerBookingResponse confirm(@PathVariable UUID bookingId) {
        return PlayerBookingResponse.of(confirmBooking.confirm(bookingId));
    }

    @PostMapping("/{bookingId}/cancel")
    PlayerBookingResponse cancel(@PathVariable UUID bookingId) {
        return PlayerBookingResponse.of(cancelOwnPendingBooking.cancel(bookingId));
    }
}
