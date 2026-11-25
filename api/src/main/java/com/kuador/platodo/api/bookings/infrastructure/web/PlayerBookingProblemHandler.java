package com.kuador.platodo.api.bookings.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.kuador.platodo.api.bookings.application.BookingExpiredException;
import com.kuador.platodo.api.bookings.application.BookingNotFoundException;
import com.kuador.platodo.api.bookings.application.BookingNotPendingException;
import com.kuador.platodo.api.bookings.application.CourtNotFoundException;
import com.kuador.platodo.api.bookings.application.InvalidOccupancyRangeException;
import com.kuador.platodo.api.bookings.application.InvalidSlotException;
import com.kuador.platodo.api.bookings.application.OwnOverlapNeedsConfirmationException;
import com.kuador.platodo.api.bookings.application.PlayerNotAuthenticatedException;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;

/** Translates the outcomes of the player booking use cases to the Problem Details of the API contract. */
@RestControllerAdvice(assignableTypes = {PlayerBookingController.class, CourtOccupancyController.class})
@ConditionalOnPlayerApiEnabled
class PlayerBookingProblemHandler {

    @ExceptionHandler(BookingOverlapException.class)
    ProblemDetail slotUnavailable() {
        return ProblemCode.SLOT_UNAVAILABLE.toProblem();
    }

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

    @ExceptionHandler(InvalidSlotException.class)
    ProblemDetail invalidSlot() {
        return ProblemCode.INVALID_SLOT.toProblem();
    }

    @ExceptionHandler(CourtNotFoundException.class)
    ProblemDetail courtNotFound() {
        return ProblemCode.COURT_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(BookingNotFoundException.class)
    ProblemDetail bookingNotFound() {
        return ProblemCode.BOOKING_NOT_FOUND.toProblem();
    }

    @ExceptionHandler(PlayerNotAuthenticatedException.class)
    ProblemDetail unauthenticated() {
        return ProblemCode.UNAUTHENTICATED.toProblem();
    }

    @ExceptionHandler(InvalidOccupancyRangeException.class)
    ProblemDetail invalidRange(InvalidOccupancyRangeException error) {
        return ProblemCode.INVALID_REQUEST.toProblem(error.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    ProblemDetail invalidRequest() {
        return ProblemCode.INVALID_REQUEST.toProblem();
    }
}
