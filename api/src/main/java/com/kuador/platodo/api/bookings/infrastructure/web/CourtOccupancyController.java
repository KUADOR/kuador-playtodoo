package com.kuador.platodo.api.bookings.infrastructure.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.bookings.application.GetCourtOccupancy;

/** {@code GET /courts/{courtId}/occupancy} of {@code contracts/bookings.openapi.yaml}. */
@RestController
@ConditionalOnPlayerApiEnabled
class CourtOccupancyController {

    private final GetCourtOccupancy getCourtOccupancy;

    CourtOccupancyController(GetCourtOccupancy getCourtOccupancy) {
        this.getCourtOccupancy = getCourtOccupancy;
    }

    @GetMapping(ApiPaths.V1 + "/courts/{courtId}/occupancy")
    List<OccupiedRangeResponse> occupancy(
            @PathVariable UUID courtId,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime to) {
        return getCourtOccupancy.occupiedPeriods(courtId, from.toInstant(), to.toInstant()).stream()
                .map(OccupiedRangeResponse::of)
                .toList();
    }
}
