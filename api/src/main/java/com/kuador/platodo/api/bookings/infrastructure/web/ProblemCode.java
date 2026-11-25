package com.kuador.platodo.api.bookings.infrastructure.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Stable error codes of the API contract ({@code ProblemCode}). Title and detail are developer-facing;
 * clients map the code to the text shown to people. Details never contain personal data.
 */
enum ProblemCode {

    SLOT_UNAVAILABLE(HttpStatus.CONFLICT, "slot-unavailable", "Slot unavailable",
            "The requested range overlaps an active booking or block of the court."),
    OWN_OVERLAP_NEEDS_CONFIRMATION(HttpStatus.CONFLICT, "own-overlap-needs-confirmation",
            "Overlapping booking of the same player",
            "Retry with acknowledgeOwnOverlap set to true to book anyway."),
    BOOKING_EXPIRED(HttpStatus.CONFLICT, "booking-expired", "Booking expired",
            "The pending hold ended and the slot was released."),
    BOOKING_NOT_PENDING(HttpStatus.CONFLICT, "booking-not-pending", "Booking is not pending",
            "Only pending bookings accept this operation."),
    INVALID_SLOT(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-slot", "Invalid slot",
            "The requested start is in the past or is not a valid slot of the court."),
    COURT_NOT_FOUND(HttpStatus.NOT_FOUND, "court-not-found", "Court not found",
            "The court does not exist in this complex."),
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "booking-not-found", "Booking not found",
            "The booking does not exist in this complex or belongs to another player."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request",
            "Malformed body or parameters."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentication required",
            "A valid player access token is required.");

    private static final String TYPE_BASE = "https://playtodoo.com/problems/";

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final String detail;

    ProblemCode(HttpStatus status, String slug, String title, String detail) {
        this.status = status;
        this.type = URI.create(TYPE_BASE + slug);
        this.title = title;
        this.detail = detail;
    }

    ProblemDetail toProblem() {
        return toProblem(detail);
    }

    ProblemDetail toProblem(String specificDetail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, specificDetail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setProperty("code", name());
        return problem;
    }
}
