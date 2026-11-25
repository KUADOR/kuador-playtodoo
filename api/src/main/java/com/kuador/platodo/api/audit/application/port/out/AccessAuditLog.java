package com.kuador.platodo.api.audit.application.port.out;

import java.util.List;

import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/** Audit trail of the Kuador accesses to the complexes, kept in the global schema. Insert only. */
public interface AccessAuditLog {

    void append(AccessAuditEvent event);

    /** One page of the events of {@code tenant}, newest first. */
    AuditPage pageOf(TenantId tenant, PageRequest page);

    /** Zero-based page index and page size. */
    record PageRequest(int page, int size) {

        public long offset() {
            return (long) page * size;
        }
    }

    record AuditPage(List<AccessAuditEvent> items, long totalItems) {

        public AuditPage {
            items = List.copyOf(items);
        }
    }
}
