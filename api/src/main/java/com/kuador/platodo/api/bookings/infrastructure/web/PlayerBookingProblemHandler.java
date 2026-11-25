package com.kuador.platodo.api.bookings.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kuador.platodo.api.bookings.application.BookingExpiredException;
import com.kuador.platodo.api.bookings.application.BookingNotPendingException;
import com.kuador.platodo.api.bookings.application.OwnOverlapNeedsConfirmationException;
import com.kuador.platodo.api.bookings.application.PlayerNotAuthenticatedException;

/**
 * Translates the outcomes only the player booking use cases have to the Problem Details of the API contract.
 * Shared outcomes are in {@link BookingProblemHandler}.
 */
@RestControllerAdvice(assignableTypes = {PlayerBookingController.class, CourtOccupancyController.class})
@ConditionalOnPlayerApiEnabled
class PlayerBookingProblemHandler {

    @ExceptionHandler(OwnOverlapNeedsConfirmationException.class)
    ProblemDetail ownOverlap(OwnOverlapNeedsConfirmationException error) {
        ProblemDetail problem = ProblemCode.OWN_OVERLAP_NEEDS_CONFIRMATION.toProblem();
        problem.setProperty("existingBooking", PlayerBookingResponse.of(error.existingBooking()));
        return problem;
    }

    @ExceptionHandler(BookingExpiredException.class)
    ProblemDetail bookingExpired() {
        return ProblemCode.BOOKING_EXPIRED.toProblem();
    }

    @ExceptionHandler(BookingNotPendingException.class)
    ProblemDetail bookingNotPending() {
        return ProblemCode.BOOKING_NOT_PENDING.toProblem();
    }

    @ExceptionHandler(PlayerNotAuthenticatedException.class)
    ProblemDetail unauthenticated() {
        return ProblemCode.UNAUTHENTICATED.toProblem();
    }
}
