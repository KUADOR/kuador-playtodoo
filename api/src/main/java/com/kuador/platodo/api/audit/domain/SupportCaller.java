package com.kuador.platodo.api.audit.domain;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * A Kuador team member asking for support access, as Identity and access sees them on this request (ADR-017,
 * ADR-020).
 *
 * @param id               platform user id
 * @param superAdmin       whether they have the {@code SUPER_ADMIN} role
 * @param tenantDataAccess whether they hold {@code TENANT_DATA_ACCESS} right now (always true for the SUPER_ADMIN)
 */
public record SupportCaller(UUID id, boolean superAdmin, boolean tenantDataAccess) {

    /** Statuses where only the SUPER_ADMIN may work, with reading and editing (ADR-020, team answer 2). */
    private static final Set<TenantStatus> SUPER_ADMIN_ONLY = EnumSet.of(TenantStatus.SUSPENDED, TenantStatus.ARCHIVED);

    public SupportCaller {
        Objects.requireNonNull(id, "id");
    }

    /**
     * Why this caller may not work on a complex in {@code status}; empty when they may. Without the capability the
     * answer is {@link DenialCode#CAPABILITY_REVOKED} whatever the status.
     */
    public Optional<DenialCode> refusalOn(TenantStatus status) {
        if (!superAdmin && !tenantDataAccess) {
            return Optional.of(DenialCode.CAPABILITY_REVOKED);
        }
        boolean allowed = status == TenantStatus.ACTIVE || superAdmin && SUPER_ADMIN_ONLY.contains(status);
        return allowed ? Optional.empty() : Optional.of(DenialCode.TENANT_NOT_ACCESSIBLE);
    }
}
