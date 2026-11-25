package com.kuador.platodo.api.audit.application;

import com.kuador.platodo.api.audit.application.port.out.AccessReasonRedaction;
import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.tenancy.domain.event.TenantPurged;

/**
 * Keeps the audit of the accesses to a purged complex without personal data (ADR-013, D10 and D11): who of Kuador,
 * when, method, route template and outcome remain; the free-text reason becomes {@link AccessReason#REDACTED}.
 * It runs inside the purge transaction, so a failure here leaves the complex not purged.
 */
public final class RedactAccessReasonsOfPurgedTenant {

    private final AccessReasonRedaction redaction;

    public RedactAccessReasonsOfPurgedTenant(AccessReasonRedaction redaction) {
        this.redaction = redaction;
    }

    public int redact(TenantPurged purged) {
        return redaction.redactReasonsOf(purged.tenantId(), AccessReason.REDACTED, purged.purgedAt());
    }
}
