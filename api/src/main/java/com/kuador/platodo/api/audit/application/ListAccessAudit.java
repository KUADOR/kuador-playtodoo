package com.kuador.platodo.api.audit.application;

import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog;
import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog.AuditPage;
import com.kuador.platodo.api.audit.application.port.out.AccessAuditLog.PageRequest;
import com.kuador.platodo.api.audit.application.port.out.CurrentPlatformOwner;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** The Kuador owner reads the audited accesses to one complex, newest first; purged complexes included. */
public final class ListAccessAudit {

    public static final int MAX_PAGE_SIZE = 100;

    private final CurrentPlatformOwner owner;
    private final TenantDirectory tenants;
    private final AccessAuditLog auditLog;

    public ListAccessAudit(CurrentPlatformOwner owner, TenantDirectory tenants, AccessAuditLog auditLog) {
        this.owner = owner;
        this.tenants = tenants;
        this.auditLog = auditLog;
    }

    /**
     * @throws PlatformUserNotAuthenticatedException if there is no authenticated caller
     * @throws NotPlatformOwnerException             if the caller is not the Kuador owner
     * @throws InvalidAuditPageException             if the page is out of the contract limits
     * @throws AccessTenantNotFoundException         if no complex has this id
     */
    public AuditPage list(TenantId tenant, int page, int size) {
        owner.require();
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidAuditPageException();
        }
        if (tenants.find(tenant).isEmpty()) {
            throw new AccessTenantNotFoundException();
        }
        return auditLog.pageOf(tenant, new PageRequest(page, size));
    }
}
