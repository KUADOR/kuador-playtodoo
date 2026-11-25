package com.kuador.platodo.api.bookings.infrastructure.web;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kuador.platodo.api.bookings.application.NotStaffOfTenantException;
import com.kuador.platodo.api.bookings.application.SlotHasBookingException;
import com.kuador.platodo.api.bookings.application.StaffNotAuthenticatedException;

/**
 * Translates the outcomes only the staff booking use cases have to the Problem Details of the API contract.
 * Shared outcomes are in {@link BookingProblemHandler}.
 */
@RestControllerAdvice(assignableTypes = StaffBookingController.class)
@ConditionalOnStaffApiEnabled
class StaffBookingProblemHandler {

    @ExceptionHandler(SlotHasBookingException.class)
    ProblemDetail slotHasBooking(SlotHasBookingException error) {
        ProblemDetail problem = ProblemCode.SLOT_HAS_BOOKING.toProblem();
        problem.setProperty("conflictingBookingId", error.conflictingBookingId());
        return problem;
    }

    @ExceptionHandler(StaffNotAuthenticatedException.class)
    ProblemDetail unauthenticated() {
        return ProblemCode.UNAUTHENTICATED.toProblem();
    }

    @ExceptionHandler(NotStaffOfTenantException.class)
    ProblemDetail forbidden() {
        return ProblemCode.FORBIDDEN.toProblem();
    }
}
