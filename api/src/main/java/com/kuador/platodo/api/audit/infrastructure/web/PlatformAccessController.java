package com.kuador.platodo.api.audit.infrastructure.web;

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

import com.kuador.platodo.api.audit.application.CloseAccessSession;
import com.kuador.platodo.api.audit.application.ListAccessAudit;
import com.kuador.platodo.api.audit.application.OpenAccessSession;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Operations of the "Platform access" tag of {@code contracts/tenancy.openapi.yaml}. Served on the global host only
 * (Tenancy keeps every {@code /platform} path there) and only to the Kuador owner.
 */
@RestController
class PlatformAccessController {

    static final String ACCESS_SESSIONS = "/api/v1/platform/tenants/{tenantId}/access-sessions";
    static final String ACCESS_SESSION = "/api/v1/platform/access-sessions/{sessionId}";
    static final String ACCESS_AUDIT = "/api/v1/platform/tenants/{tenantId}/access-audit";

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final OpenAccessSession openAccessSession;
    private final CloseAccessSession closeAccessSession;
    private final ListAccessAudit listAccessAudit;

    PlatformAccessController(OpenAccessSession openAccessSession, CloseAccessSession closeAccessSession,
            ListAccessAudit listAccessAudit) {
        this.openAccessSession = openAccessSession;
        this.closeAccessSession = closeAccessSession;
        this.listAccessAudit = listAccessAudit;
    }

    @PostMapping(ACCESS_SESSIONS)
    @ResponseStatus(HttpStatus.CREATED)
    AccessSessionResponse open(@PathVariable UUID tenantId, @RequestBody OpenAccessSessionRequest request) {
        return AccessSessionResponse.of(openAccessSession.open(tenantOf(tenantId), request.reason()));
    }

    @DeleteMapping(ACCESS_SESSION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void close(@PathVariable UUID sessionId) {
        closeAccessSession.close(sessionId);
    }

    @GetMapping(ACCESS_AUDIT)
    AccessAuditPageResponse audit(@PathVariable UUID tenantId,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return AccessAuditPageResponse.of(listAccessAudit.list(tenantOf(tenantId), page, size), page, size);
    }

    private static TenantId tenantOf(UUID tenantId) {
        return new TenantId(tenantId.toString());
    }
}
