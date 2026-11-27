package com.kuador.platodo.api.audit.infrastructure.web;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.audit.application.CloseSupportSession;
import com.kuador.platodo.api.audit.application.ListAuditEntries;
import com.kuador.platodo.api.audit.application.OpenSupportSession;
import com.kuador.platodo.api.audit.domain.AuditedRequest;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Operations of the "Support access" tag of {@code contracts/platform-backoffice.openapi.yaml} (ADR-020). Served on
 * the global host only (Tenancy keeps every {@code /platform} path there).
 */
@RestController
class SupportAccessController {

    static final String SUPPORT_SESSIONS = "/api/v1/platform/complexes/{complexId}/support-sessions";
    static final String SUPPORT_SESSION = "/api/v1/platform/support-sessions/{sessionId}";
    static final String AUDIT_ENTRIES = "/api/v1/platform/complexes/{complexId}/audit-entries";

    /** The opening and the closing of a session are audited as the contract operations that do them. */
    private static final AuditedRequest OPENING =
            new AuditedRequest("POST", SUPPORT_SESSIONS, Optional.of("openSupportSession"));
    private static final AuditedRequest CLOSING =
            new AuditedRequest("DELETE", SUPPORT_SESSION, Optional.of("closeSupportSession"));

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final OpenSupportSession openSupportSession;
    private final CloseSupportSession closeSupportSession;
    private final ListAuditEntries listAuditEntries;

    SupportAccessController(OpenSupportSession openSupportSession, CloseSupportSession closeSupportSession,
            ListAuditEntries listAuditEntries) {
        this.openSupportSession = openSupportSession;
        this.closeSupportSession = closeSupportSession;
        this.listAuditEntries = listAuditEntries;
    }

    @PostMapping(SUPPORT_SESSIONS)
    @ResponseStatus(HttpStatus.CREATED)
    SupportSessionResponse open(@PathVariable UUID complexId, @RequestBody OpenSupportSessionRequest request) {
        return SupportSessionResponse.of(openSupportSession.open(tenantOf(complexId), request.reason(), OPENING));
    }

    @DeleteMapping(SUPPORT_SESSION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void close(@PathVariable UUID sessionId) {
        closeSupportSession.close(sessionId, CLOSING);
    }

    @GetMapping(AUDIT_ENTRIES)
    AuditEntryPageResponse auditEntries(@PathVariable UUID complexId,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return AuditEntryPageResponse.of(listAuditEntries.list(tenantOf(complexId), page, size), page, size);
    }

    private static TenantId tenantOf(UUID complexId) {
        return new TenantId(complexId.toString());
    }
}
