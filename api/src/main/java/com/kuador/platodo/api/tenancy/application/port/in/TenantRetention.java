package com.kuador.platodo.api.tenancy.application.port.in;

/**
 * Changes the retention period of a suspended or archived complex (ADR-014): its purge date is recomputed from the
 * date the retention started. Joins the global transaction of the caller, so the caller can lock the complex first.
 */
@FunctionalInterface
public interface TenantRetention {

    /**
     * @param days at least {@link com.kuador.platodo.api.tenancy.domain.Tenant#MIN_RETENTION_DAYS}
     * @throws com.kuador.platodo.api.tenancy.application.TenantNotFoundException     if no complex has this id
     * @throws com.kuador.platodo.api.tenancy.domain.NotInRetentionException         if it is neither suspended nor
     *                                                                                archived
     * @throws com.kuador.platodo.api.tenancy.domain.RetentionInvalidException       if the new purge date would not be
     *                                                                                in the future
     */
    void changeRetention(TenantId tenant, int days);
}
