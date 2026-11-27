package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.util.Map;
import java.util.Optional;

/**
 * The operationId of each contract operation served on the host of a complex ({@code contracts/bookings.openapi.yaml}
 * and the tenant-host operations of {@code contracts/tenancy.openapi.yaml}), so the audit of a support request names
 * the operation it called (ADR-020). {@code TenantHostOperationsContractTest} keeps this table equal to the
 * contracts.
 */
final class TenantHostOperations {

    private static final String V1 = TenancyApiPaths.V1;

    private static final Map<String, String> OPERATION_IDS = Map.ofEntries(
            operation("GET", "/courts/{courtId}/occupancy", "getCourtOccupancy"),
            operation("POST", "/bookings", "createPendingBooking"),
            operation("GET", "/bookings/{bookingId}", "getBooking"),
            operation("POST", "/bookings/{bookingId}/confirm", "confirmBooking"),
            operation("POST", "/bookings/{bookingId}/cancel", "cancelOwnPendingBooking"),
            operation("GET", "/staff/bookings", "listStaffBookings"),
            operation("POST", "/staff/bookings", "createStaffBooking"),
            operation("POST", "/staff/blocks", "blockSlot"),
            operation("GET", "/staff/bookings/{bookingId}", "getStaffBooking"),
            operation("POST", "/staff/bookings/{bookingId}/cancel", "cancelBookingByStaff"),
            operation("GET", "/tenant", "getTenantState"),
            operation("PUT", "/tenant/subdomain", "changeTenantSubdomain"),
            operation("POST", "/auth/staff/login", "loginStaff"));

    private TenantHostOperations() {
    }

    /** The operationId of {@code httpMethod} on {@code routeTemplate}; empty when it is not a contract operation. */
    static Optional<String> operationIdOf(String httpMethod, String routeTemplate) {
        return Optional.ofNullable(OPERATION_IDS.get(key(httpMethod, routeTemplate)));
    }

    /** Every operation, keyed as {@code "<METHOD> <route template>"}; for the contract test. */
    static Map<String, String> all() {
        return OPERATION_IDS;
    }

    private static Map.Entry<String, String> operation(String httpMethod, String contractPath, String operationId) {
        return Map.entry(key(httpMethod, V1 + contractPath), operationId);
    }

    private static String key(String httpMethod, String routeTemplate) {
        return httpMethod + " " + routeTemplate;
    }
}
